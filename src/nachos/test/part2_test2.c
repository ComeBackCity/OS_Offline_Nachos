#include "stdio.h"
#include "stdlib.h"
#include "syscall.h"

int main(int argc, char** argv)
{
    int i,j,num;
    char buf[30];

    printf("\n\n---------------Inside Test 2-----------\n\n");
    printf("\nENTER NUMBER: ");
    readline(buf,10);
    num = atoi(buf);

    printf("\nUser Input: %d",num);
    printf("\nMultiplication Table for %d ",num);

    for(i=1;i<=10;i++){
        printf("\n%d * %d = %d",i,num,i*num);
    }
    printf("\n---------------Finished Test 2-----------\n\n");




    return 0;
}