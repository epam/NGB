package com.epam.catgenome.manager;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Aspect
@Component
public class DeadlockDetectAspect implements Ordered {

	public static final Logger logger = LoggerFactory.getLogger(DeadlockDetectAspect.class);
	private static final int RETRY_ASPECT_ORDER = 99;

	/** Order for this aspect, should be lower than for transaction manager which has 100 **/
	protected int order = RETRY_ASPECT_ORDER;

	/** How many retries should be tried on deadlock **/
	@Value("#{catgenome['transaction.retry.attempts'] ?: 3}")
	private int retryCount;

	/** How big is delay between deadlock retry (in ms) **/
	@Value("#{catgenome['transaction.retry.delay'] ?: 500}")
	private int delay;

	@Around(value = "@annotation(org.springframework.transaction.annotation.Transactional)")
	public Object methodRetry(ProceedingJoinPoint pjp) throws Throwable {
		return detectDeadlocks(pjp);
	}

	@Around(value = "@within(org.springframework.transaction.annotation.Transactional)")
	public Object classRetry(ProceedingJoinPoint pjp) throws Throwable {
		return detectDeadlocks(pjp);
	}

	protected Object detectDeadlocks(ProceedingJoinPoint pjp) throws Throwable {
		if (logger.isTraceEnabled()) {
			logger.trace("Before pointcut {} with transaction manager active: {}",
					pjp.toString(), TransactionSynchronizationManager.isActualTransactionActive());
		}

		try {
			int retryCount = getRetryCount();
			while (true) {
				try {
					return pjp.proceed();
				} catch (DataAccessException ex) {
					// if transaction manager is active, this means that we are in nested @Transactional call,
					// but we want only make retry for the main @Transactional call, that starts the transaction again
					if (TransactionSynchronizationManager.isActualTransactionActive()) {
						if (logger.isTraceEnabled()) {
							logger.trace("Deadlock pointcut detected, but transaction is still active - propagating");
						}
						throw ex;
					} else {
						// throw exception to upper layer
						if (retryCount-- == 0) {
							throw ex;
						}

						// otherwise, try to repeat this step
						if (logger.isDebugEnabled()) {
							logger.debug("Deadlock pointcut retry with retryCount={} (sleeping {} ms)",
									retryCount, getDelay());
						}

						Thread.sleep(getDelay());
					}
				}
			}
		} finally {
			if (logger.isTraceEnabled()) {
				logger.trace("After pointcut {} with transaction manager active: {}",
						pjp.toString(), TransactionSynchronizationManager.isActualTransactionActive());
			}
		}
	}

	@Override
	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public int getRetryCount() {
		return retryCount;
	}

	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}

	public int getDelay() {
		return delay;
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}
}