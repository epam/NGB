import os

from dash import Dash, html, dash_table, dcc, callback, Output, Input
import pandas as pd
import plotly.express as px
import dash_bootstrap_components as dbc
from datetime import date, timedelta

STYLE_HIDE = {'display': 'none'}
STYLE_SHOW = {'display': 'block'}
LAST_WEEK = 'Last week'
LAST_MONTH = 'Last month'
LAST_YEAR = 'Last year'
THIS_YEAR = 'This year'
CUSTOM_PERIOD = 'Custom period'

data_file = os.getenv('NGB_STATS_FILE')
if not data_file or not os.path.exists(data_file):
    raise ValueError('Missing user activity data')
df = pd.read_parquet(data_file)
df['date'] = pd.to_datetime(df['date'])
df = df.reset_index(drop=True)

external_stylesheets = [dbc.themes.BOOTSTRAP]
app = Dash(__name__, external_stylesheets=external_stylesheets)

dropdown = dcc.Dropdown(
            id='timeframe_dropdown',
            multi=False,
            options=[
                {'label': LAST_WEEK, 'value': LAST_WEEK},
                {'label': LAST_MONTH, 'value': LAST_MONTH},
                {'label': LAST_YEAR, 'value': LAST_YEAR},
                {'label': THIS_YEAR, 'value': THIS_YEAR},
                {'label': CUSTOM_PERIOD, 'value': CUSTOM_PERIOD}
            ],
            value=THIS_YEAR,
            clearable=False,
            style={"width": "150px", "margin-right": "15px"}
    )


date_picker = dcc.DatePickerRange(
            id='date-picker-range',
            min_date_allowed=date(2020, 1, 1),
            initial_visible_month=date.today(),
            persistence=True
        )

# App layout
app.layout = dbc.Container([
    dbc.Row([
        html.Div('NGB User Statistics', style={"font-weight": "bold", "font-size": "large", "margin-bottom": "10px"})
    ]),

    html.Div([dropdown, date_picker], style={"display": "flex", "align-items": "center"}),

    html.Table(className='table',
               children=
               [
                   html.Tr([html.Td(id='unique-users-cell', style={'width': '200px'}),
                            html.Td(id='number-logins-cell', style={'width': '200px'}),
                            html.Td(id='active-users-cell', style={'width': '200px'})]),
               ], style=STYLE_SHOW, id='total-stat-table'),

    dbc.Row([
        dbc.Col([
            dcc.Graph(figure={}, id='user-activity-chart')
        ]),
    ], style=STYLE_SHOW, id='chart-container'),

    dbc.Row([
        dbc.Col([
            html.Label("Logins per user"),
            dash_table.DataTable(data=[], page_size=12, style_table={'overflowX': 'auto'},
                                 sort_action='native', id='user-table', export_format='csv')
        ]),
    ], style=STYLE_SHOW, id='user-table-container'),

], fluid=True)


def previous_week_end(input_date):
    weekday_index = input_date.isoweekday()
    # Sunday case
    delta = 7 if weekday_index == 7 else 0
    return input_date - timedelta(days=((input_date.isoweekday()) % 7 + delta))


def previous_week(input_date):
    end = previous_week_end(input_date)
    return end - timedelta(days=6), end


def get_previous_month(input_date):
    first_day_of_current_month = input_date.replace(day=1)
    end = first_day_of_current_month - timedelta(days=1)
    return end.replace(day=1), end


def get_previous_year(input_date):
    previous = input_date.replace(year=input_date.year - 1)
    return previous.replace(month=1, day=1), previous.replace(month=12, day=31)


def get_this_year(input_date):
    return input_date.replace(month=1, day=1), input_date.replace(month=12, day=31)


@callback(
    Output('date-picker-range', 'start_date'),
    Output('date-picker-range', 'end_date'),
    Input('timeframe_dropdown', 'value')
)
def update_date(value):
    today = date.today()
    start = ''
    end = ''
    if value == LAST_WEEK:
        start, end = previous_week(today)
    if value == LAST_MONTH:
        start, end = get_previous_month(today)
    if value == LAST_YEAR:
        start, end = get_previous_year(today)
    if value == THIS_YEAR:
        start, end = get_this_year(today)
    if value == CUSTOM_PERIOD:
        return None, None
    return str(start), str(end)

@callback(
    Output('user-activity-chart', 'figure'),
    Output('user-table', 'data'),
    Output('chart-container', 'style'),
    Output('total-stat-table', 'style'),
    Output('user-table-container', 'style'),
    Output('unique-users-cell', 'children'),
    Output('number-logins-cell', 'children'),
    Output('active-users-cell', 'children'),
    Input('date-picker-range', 'start_date'),
    Input('date-picker-range', 'end_date'))
def update_output(start_date, end_date):
    if not start_date or not end_date:
        return {}, [], STYLE_HIDE, STYLE_HIDE, STYLE_HIDE, '', '', ''
    start_date_object = pd.to_datetime(start_date)
    end_date_object = pd.to_datetime(end_date)
    data = df[(df['date'] >= start_date_object) & (df['date'] <= end_date_object)]

    graph_data = data.groupby("date").agg(
        NumerOfUsers=pd.NamedAgg(column="user", aggfunc="count"),
        NumberOfLogins=pd.NamedAgg(column="logins", aggfunc="sum"),
        ActiveTime=pd.NamedAgg(column="duration", aggfunc="sum")
    )
    graph_data['date'] = graph_data.index
    fig = px.line(graph_data, x="date", y="NumerOfUsers", title='Logins per day',
                  labels={"NumerOfUsers": "Logins count", "date": ""},
                  template='plotly_white', markers=True)
    fig.update_traces(line_color='#000000')

    processed = data.groupby("user").agg(
        User=pd.NamedAgg(column="user", aggfunc="first"),
        Email=pd.NamedAgg(column="email", aggfunc="first"),
        LastLogin=pd.NamedAgg(column="date", aggfunc="max"),
        NumberOfLogins=pd.NamedAgg(column="logins", aggfunc="sum"),
        ActiveTime=pd.NamedAgg(column="duration", aggfunc="sum")
    )
    processed['LastLogin'] = pd.to_datetime(processed['LastLogin']).dt.date
    processed = processed.rename(columns={'LastLogin': 'Last Login Date',
                                          'NumberOfLogins': 'Number of Logins',
                                          'ActiveTime': 'Duration Used (min)'})

    unique_users = len(data['user'].unique())
    total_logins = data['logins'].sum()
    active_users_data = data[(df['date'] >= end_date_object - timedelta(weeks=8))]
    active_users = len(active_users_data['user'].unique())

    return fig, processed.to_dict('records'), \
           STYLE_SHOW, STYLE_SHOW, STYLE_SHOW, \
           'Unique Users: %d' % unique_users, \
           'Number of Logins: %d' % total_logins, \
           'Number of Active Users: %d' % active_users


# Run the app
if __name__ == '__main__':
    app.run(debug=True)
