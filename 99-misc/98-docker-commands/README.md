# Docker Commands

## Docker Core

### Show Log

```
docker logs -f <container-name>
```

### Stopping a container

To stop a container, run the following command

```
docker stop <container-name>
```

The container is only stopped, but will still be around and could be started again. You can use `docker ps -a` to view also containers, which are stopped.

### Running a command inside a running container

To run a command in a running container

```
docker exec -ti <container-name> bash
```

### Remove all unused local volumes

```
docker volume prune
```

## Docker Compose

To get the logs for all of the services running inside the docker compose environment, perform

```
docker-compose logs -f
```

if you only want to see the logs for one of the services, say of `connect-1`, perform


```
docker-compose logs -f connect-1
```

To see the log of multiple services, just list them as shown below

```
docker-compose logs -f connect-1 connect-2
```

