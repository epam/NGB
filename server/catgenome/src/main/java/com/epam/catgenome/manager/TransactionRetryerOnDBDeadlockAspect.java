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

@Aspect
@Component
public class TransactionRetryerOnDBDeadlockAspect implements Ordered {

	public static final Logger logger = LoggerFactory.getLogger(TransactionRetryerOnDBDeadlockAspect.class);

	private static final int ASPECT_ORDER = -1;

	/** Order for this aspect, should be lower than for transaction manager which has 100 **/
	protected int order = ASPECT_ORDER;

	/** How many retries should be tried on deadlock **/
	@Value("#{catgenome['transaction.retry.attempts'] ?: 3}")
	private int maxRetryCount;

	/** How big is delay between deadlock retry (in ms) **/
	@Value("#{catgenome['transaction.retry.delay'] ?: 500}")
	private int delay;

	@Around(value = "@annotation(org.springframework.transaction.annotation.Transactional)")
	public Object proceedTransactionWithRetry(ProceedingJoinPoint pjp) throws Throwable {
		if (logger.isDebugEnabled()) {
			logger.debug("Try to proceed transaction");
		}
		int numAttempts = 0;
		DataAccessException dataAccessException;
		do {

			if (logger.isDebugEnabled() && numAttempts > 0) {
				logger.debug("Try to proceed transaction with after failure. Attempt = %d", numAttempts);
			}

			numAttempts++;
			try {
				return pjp.proceed();
			}
			catch(DataAccessException ex) {
				dataAccessException = ex;
			}
		}
		while(numAttempts <= this.maxRetryCount);

		logger.error("All transaction attempts failed, %s will thrown", dataAccessException.getClass().getName());
		throw dataAccessException;
	}

	@Override
	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public int getMaxRetryCount() {
		return maxRetryCount;
	}

	public void setMaxRetryCount(int maxRetryCount) {
		this.maxRetryCount = maxRetryCount;
	}

	public int getDelay() {
		return delay;
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}
}