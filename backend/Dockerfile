FROM golang:1.22-alpine AS build
WORKDIR /src
COPY go.mod ./
COPY *.go ./
RUN go build -o /api .

FROM alpine:3.20
RUN apk add --no-cache wget
COPY --from=build /api /usr/local/bin/api
EXPOSE 8081
CMD ["api"]
