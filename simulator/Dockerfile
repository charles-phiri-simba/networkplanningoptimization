FROM golang:1.22-alpine AS build
WORKDIR /src
COPY go.mod ./
COPY internal ./internal
COPY cmd ./cmd
RUN go mod tidy && go build -o /simulator ./cmd/simulator

FROM alpine:3.20
COPY --from=build /simulator /simulator
ENTRYPOINT ["/simulator"]
