#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <inttypes.h>
#include "rocc.h"
#include "encoding.h"

#define DIM 4

void matrix_mult_soft(int32_t A[DIM][DIM], int32_t B[DIM][DIM], int64_t C[DIM][DIM]) {
    for (int i = 0; i < DIM; i++) {
        for (int j = 0; j < DIM; j++) {
            C[i][j] = 0;
            for (int k = 0; k < DIM; k++) {
                C[i][j] += (int64_t)A[i][k] * (int64_t)B[k][j];
            }
        }
    }
}

static inline uint64_t pack_2x32(int32_t a, int32_t b) {
    return ((uint64_t)(uint32_t)a) | ((uint64_t)(uint32_t)b << 32);
}

void load_matrixA(int32_t matrix[DIM][DIM]) {
    // 行优先加载，每次加载一行中的两个元素
    for (int i = 0; i < DIM; i++) {
        for (int j = 0; j < DIM; j += 2) {
            uint64_t packed = pack_2x32(matrix[i][j], matrix[i][j+1]);
            ROCC_INSTRUCTION_SS(0, packed, 0, 0); // custom0, funct=0
        }
    }
}

void load_matrixB(int32_t matrix[DIM][DIM]) {
    // 列优先加载，每次加载一列中的两个元素
    for (int col = 0; col < DIM; col++) {
        for (int row = 0; row < DIM; row += 2) {
            uint64_t packed = pack_2x32(matrix[row][col], matrix[row+1][col]);
            ROCC_INSTRUCTION_SS(0, packed, 0, 1); // custom0, funct=1
        }
    }
}

void get_hard_result(int64_t result[DIM][DIM]) {
    for (int i = 0; i < DIM; i++) {
        for (int j = 0; j < DIM; j++) {
            uint64_t val;
            ROCC_INSTRUCTION_D(0, val, 3); // custom0, funct=3
            result[i][j] = (int64_t)val;
        }
    }
}

void print_matrix(const char* name, int64_t mat[DIM][DIM]) {
    printf("%s:\n", name);
    for (int i = 0; i < DIM; i++) {
        for (int j = 0; j < DIM; j++) {
            printf("%12" PRId64 " ", mat[i][j]);
        }
        printf("\n");
    }
}


int main() {
    int32_t A[DIM][DIM] = {
        {1, 2, 3, 4},
        {5, 6, 7, 8},
        {9, 10, 11, 12},
        {13, 14, 15, 16}
    };
    
    int32_t B[DIM][DIM] = {
        {17, 18, 19, 20},
        {21, 22, 23, 24},
        {25, 26, 27, 28},
        {29, 30, 31, 32}
    };

    int64_t C_soft[DIM][DIM], C_hard[DIM][DIM];
    unsigned long  start, end;

    start = rdcycle();
    matrix_mult_soft(A, B, C_soft);
    end = rdcycle();
    printf("Software calculation completed. Average time used: %lu \n\n", (end - start));
    
    start = rdcycle();
    load_matrixA(A);
    load_matrixB(B);
    ROCC_INSTRUCTION(0, 2); // custom0, funct=2触发计算
    get_hard_result(C_hard);
    end = rdcycle();
    printf("Hardware calculation completed. Average time used: %lu \n\n", (end - start));
    
    // 验证结果
    int errors = 0;
    for (int i = 0; i < DIM; i++) {
        for (int j = 0; j < DIM; j++) {
            if (C_soft[i][j] != C_hard[i][j]) {
                printf("Mismatch at [%d][%d]: soft=%" PRId64 ", hard=%" PRId64 "\n",
                       i, j, C_soft[i][j], C_hard[i][j]);
                errors++;
            }
        }
    }

    print_matrix("Software Result", C_soft);
    print_matrix("Hardware Result", C_hard);
    printf("Verification %s! Errors: %d\n", errors ? "FAILED" : "PASSED", errors);

    return errors ? EXIT_FAILURE : EXIT_SUCCESS;
}