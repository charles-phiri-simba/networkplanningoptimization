package main

import (
	"context"
	"flag"
	"fmt"
	"os"
	"strings"
	"time"

	"github.com/charles-phiri-simba/networkplanningoptimization/simulator/internal/scenario"
	"github.com/segmentio/kafka-go"
)

func main() {
	scenarioName := flag.String("scenario", envOr("SNIP_SCENARIO", "high-bler-load"), "scenario name")
	brokers := flag.String("brokers", envOr("SNIP_KAFKA_BROKERS", "127.0.0.1:9092"), "comma-separated Kafka brokers")
	topic := flag.String("topic", envOr("SNIP_TELEMETRY_TOPIC", "snip.telemetry.cell-kpi.v1"), "Kafka topic")
	flag.Parse()

	events, err := scenario.Build(*scenarioName)
	if err != nil {
		fmt.Fprintf(os.Stderr, "simulator: %v\n", err)
		os.Exit(1)
	}

	writer := &kafka.Writer{
		Addr:     kafka.TCP(strings.Split(*brokers, ",")...),
		Topic:    *topic,
		Balancer: &kafka.Hash{},
	}
	defer writer.Close()

	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	msgs := make([]kafka.Message, 0, len(events))
	for _, evt := range events {
		body, err := evt.MarshalJSONCanonical()
		if err != nil {
			fmt.Fprintf(os.Stderr, "simulator: %v\n", err)
			os.Exit(1)
		}
		msgs = append(msgs, kafka.Message{
			Key:   []byte(evt.KafkaKey()),
			Value: body,
		})
		fmt.Printf("publish eventId=%s cellId=%s metric=%s value=%v key=%s\n", evt.EventID, evt.CellID, evt.Metric, evt.Value, evt.KafkaKey())
	}
	if err := writer.WriteMessages(ctx, msgs...); err != nil {
		fmt.Fprintf(os.Stderr, "simulator: kafka publish failed: %v\n", err)
		os.Exit(1)
	}
	fmt.Printf("published %d events scenario=%s topic=%s\n", len(msgs), *scenarioName, *topic)
}

func envOr(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
