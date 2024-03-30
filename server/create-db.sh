IMAGE=pns

docker build --tag $IMAGE .

mkdir application/instance
docker run -v $PWD/application:/home/flask/server  --rm -it $IMAGE python backend/data/db.py

docker image rm $IMAGE