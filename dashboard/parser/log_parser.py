import json
import os
import pandas as pd
import datetime

DATE_FORMAT = "%Y-%m-%d"
LOG_PREFIX = 'requests.log.'
ANONYMOUS = 'Anonymous User'
DEFAULT_SESSION = 'default'


def parse_message(data, timestamp):
    trimmed = data[len('After request ['):-1]
    chunks = trimmed.split(';')
    result = {'timestamp': datetime.datetime.strptime(timestamp, "%d/%m/%Y %H:%M:%S")}
    for chunk in chunks:
        if chunk.startswith('session'):
            result['session'] = chunk.split('=')[1]
        if chunk.startswith('user'):
            result['user'] = chunk.split('=')[1]
    if 'user' not in result:
         result['user'] =  ANONYMOUS
         result['session'] = DEFAULT_SESSION
    return result


def calculate_stats(entry):
    grouped = entry.groupby('session').agg(
        session_start=pd.NamedAgg(column="timestamp", aggfunc="min"),
        session_end=pd.NamedAgg(column="timestamp", aggfunc="max"),
    )
    grouped['duration'] = grouped['session_end'] - grouped['session_start']
    return grouped['duration'].sum()


def process_dataset(df):
    result = df.groupby("user").agg(
        date=pd.NamedAgg(column="timestamp", aggfunc="max"),
        logins=pd.NamedAgg(column="session", aggfunc=pd.Series.nunique),
    )
    result['duration'] = df.groupby("user").apply(lambda x: calculate_stats(x))
    result['email'] = result.index
    result['user'] = result.index
    result['date'] = pd.to_datetime(result['date']).dt.date
    result['duration'] = result['duration'].apply(lambda x: x.total_seconds() / 60.0).round(2)
    return result


def parse_logs(log_folder, output, last_sync, sync_token):
    data = None
    last_sync_date = None
    for file in os.listdir(log_folder):
        if file.startswith(LOG_PREFIX):
            file_date = datetime.datetime.strptime(file[len(LOG_PREFIX):], "%Y-%m-%d")
            if last_sync:
                if file_date <= last_sync:
                    continue
            print('Reading file ' + file)
            with open(os.path.join(log_folder, file)) as log:
                items = []
                for line in log.readlines():
                    line = line.strip()
                    if line:
                        if 'payload' in line:
                            index = line.index('payload')
                            line = line[:index] + '"}'
                        try:
                            parsed = json.loads(line)
                            timestamp = parsed['debug_timestamp']
                            entry = parse_message(parsed['debug_message'], timestamp)
                            if 'user' not in entry:
                                continue
                            items.append(entry)
                        except BaseException as e:
                            pass
                if len(items) == 0:
                    continue
                chunk = process_dataset(pd.DataFrame.from_dict(items))
                if data is None:
                    data = chunk
                else:
                    data = pd.concat([data, chunk])
                if last_sync_date is None:
                    last_sync_date = file_date
                else:
                    if file_date > last_sync_date:
                        last_sync_date = file_date
    print("Finished reading request data from %s. Processed %d entries. Last synchronized date is %s."
          % (log_folder, 0 if data is None else data.size, last_sync_date))

    if os.path.exists(output):
        previous_data = pd.read_parquet(output)
        result = pd.concat([previous_data, data])
    else:
        result = data

    if result is not None:
        print('Saving usage statistics to %s.' % output)
        result.to_parquet(output)

        if sync_token and last_sync_date:
            with open(sync_token, 'w') as token:
                token.write(last_sync_date.strftime(DATE_FORMAT) + "\n")


def read_last_sync(file_path):
    if file_path is None or not os.path.exists(file_path):
        return None
    with open(file_path) as token:
        for line in token.readlines():
            if line:
                try:
                    return datetime.datetime.strptime(line.strip(), DATE_FORMAT)
                except:
                    pass
    return None


if __name__ == '__main__':
    folder = os.getenv('NGB_LOG_FOLDER')
    output = os.getenv('NGB_STATS_FILE')
    last_sync = os.getenv('NGS_LOG_SYNC_TIMESTAMP', None)
    if not folder:
        raise ValueError('Log folder not specified')
    if not output:
        raise ValueError('Output folder not specified')
    if not os.path.exists(folder):
        raise ValueError('Provided folder %s does not exist' % folder)
    if not os.path.exists(os.path.dirname(output)):
        os.makedirs(os.path.dirname(output))
    parse_logs(folder, output, read_last_sync(last_sync), last_sync)
