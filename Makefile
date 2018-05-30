run: stop
	docker-compose up

stop:
	docker-compose down

build:
	docker build \
		--tag evoila/mongodb \
		--file Dockerfile \
		.
