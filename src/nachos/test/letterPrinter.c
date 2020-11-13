#include "stdio.h"
#include "stdlib.h"
#include "syscall.h"

int main(int argc, char** argv)
{
    int i,j;
    volatile num=0;

    for(i=0;i<26;i++)
    {
        int letter = 'a' + i;
        char ch = (char) letter;
        for(j=0;j<10000000;j++)
        {
            num += j;
        }
        printf("\n%c ",ch);
    }

    //printf("%d",num);
    printf("\n\n");
    return 0;
}