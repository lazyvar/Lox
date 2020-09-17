CC := gcc
CFLAGS := -std=c99 -Wall -Wextra -Wno-unused-variable -O0 -DDEBUG -g
SOURCES := $(wildcard *.c)

build: $(SOURCES)
	@ mkdir -p build
	@ $(CC) -o build/clox -$(CFLAGS) $(SOURCES)
