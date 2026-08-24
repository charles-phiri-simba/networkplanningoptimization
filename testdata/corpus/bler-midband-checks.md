# Sample planning note — high BLER on a mid-band cell

Source-id: sample-bler-midband
Locator: section-1

When block error rate (BLER) is high on a mid-band cell, check these items before changing the live radio:

1. Confirm the measurement window and that BLER is persistently elevated, not a single busy-hour spike.
2. Compare uplink and downlink BLER. A downlink-only rise often points to coverage or interference; an uplink-only rise often points to UE power or uplink interference.
3. Inspect neighbouring mid-band cells for overlapping coverage and PCI confusion.
4. Review scheduled throughput versus offered load. Congestion can raise BLER without a radio-fault.
5. Check recent configuration changes: power, beam, bandwidth part, or handover thresholds.

Do not apply tilt, power, or frequency changes from this note. Record findings and recommend a human review.
