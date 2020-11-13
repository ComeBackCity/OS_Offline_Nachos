#include "stdio.h"
#include "stdlib.h"
#include "syscall.h"

int main(int argc, char** argv)
{
    int i,j;
    volatile num=0;

    for(i=0;i<26;i++)
    {
        for(j=0;j<10000000;j++)
        {
           num += j;
        }
        printf("\n%d ",(i%10));
    }

    //printf("%d",num);
    printf("\n\n");

    return 0;
}