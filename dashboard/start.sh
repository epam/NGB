cron
. $ANACONDA_HOME/etc/profile.d/conda.sh
conda activate dashboard
cd $DASHBOARD_HOME/dashboard
nohup python3 app.py >> $DASHBOARD_HOME/app.log 2>&1 &