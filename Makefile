
test:
	docker pull mozilla/sbt
	docker run -it --rm -v $(CURDIR)/my-scalatra-web-app:/app -w /app mozilla/sbt sbt test
