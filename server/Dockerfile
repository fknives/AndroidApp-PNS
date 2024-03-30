FROM python:3.9-bullseye

# ENV setup
RUN ["python","--version"]
RUN ["apt-get", "update"]
RUN ["pip", "install", "uwsgi"]
RUN ["uwsgi", "--version"]
RUN ["pip", "install", "flask"]
RUN ["pip", "install", "passlib"]
RUN ["pip", "install", "firebase_admin"]
RUN ["pip", "install", "pycryptodome"]
RUN ["pip", "install", "watchdog"]

RUN adduser --system --no-create-home flask
USER flask

WORKDIR /home/flask/server

# load source files
COPY ./application /home/flask/server

CMD uwsgi --ini notification-service.ini --touch-reload=notification-service.ini