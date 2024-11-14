# Cafe ring-buffer

Simple implementation of a ring-buffer in `Java`.

## Setup

add `--add-opens java.base/java.nio=ALL-UNNAMED` as a JVM argument

## Reference

- `False sharing`: [https://en.wikipedia.org/wiki/False_sharing](https://theboreddev.com/understanding-false-sharing)
- `Happens before guarantee`: https://jenkov.com/tutorials/java-concurrency/java-happens-before-guarantee.html
- `Ring buffer`: https://aeron.io/docs/agrona/concurrent/#ring-buffers
