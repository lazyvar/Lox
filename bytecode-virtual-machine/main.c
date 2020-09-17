#include "common.h"
#include "chunk.h"
#include "debug.h"

int main(int argc, const char* argv[]) {
  Chunk chunk;

  initChunk(&chunk);

  int c1 = addConstant(&chunk, 3.33);
  int c2 = addConstant(&chunk, 100);

  writeChunk(&chunk, OP_CONSTANT, 1);
  writeChunk(&chunk, c1, 1);
  writeChunk(&chunk, OP_RETURN, 1);
  writeChunk(&chunk, OP_CONSTANT, 2);
  writeChunk(&chunk, c2, 2);
  writeChunk(&chunk, OP_RETURN, 2);

  disassembleChunk(&chunk, "cambel's soup");

  freeChunk(&chunk);

  return 0;
}
