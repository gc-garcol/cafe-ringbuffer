# Cafe ring-buffer

Simple implementation of a ring-buffer in `Java`.

## Features

- [X] `OneToManyRingBuffer` (also configurable for `OneToOneRingBuffer` usage)
- [ ] `ManyToOneRingBuffer`
- [ ] `ManyToManyRingBuffer`

## Setup

add `--add-opens java.base/java.nio=ALL-UNNAMED` as a JVM argument

## RingBuffer structure

### RingBuffer message structure

```mermaid
block-beta
    1["header: message length"]:1
    2["header: message type"]:1
    4("message"):4
    5["padding"]:1
style 4 fill:#fcb,stroke:#333,color:#fff
style 5 fill:#d6d,stroke:#333
```

## Reference

- `False sharing`:
  - https://trishagee.com/2011/07/22/dissecting_the_disruptor_why_its_so_fast_part_two__magic_cache_line_padding/
  - https://mechanical-sympathy.blogspot.com/2011/07/false-sharing.html
- `Happens before guarantee`: https://jenkov.com/tutorials/java-concurrency/java-happens-before-guarantee.html
- `Ring buffer`: https://aeron.io/docs/agrona/concurrent/#ring-buffers
