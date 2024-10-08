FROM python:3.9-bullseye

ARG UID=1000
ARG GID=1000

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

RUN groupadd -g $GID -o flask
RUN adduser -u $UID --gid $GID --no-create-home flask
USER flask

WORKDIR /home/flask/server

# load source files
COPY ./application /home/flask/server

CMD uwsgi --ini notification-service.ini --touch-reload=notification-service.ini
