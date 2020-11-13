#include "stdio.h"
#include "stdlib.h"
#include "syscall.h"

int main(int argc, char** argv)
{
    int i,j,num;
    volatile vol=0;
    char buf[30];
    char *execArgs[256];
    int status1,processID, processID1, processID2, status2,k,l;


    printf("Hello world\n");

    printf("Beginning of the end\n");

    printf("\n---------------TESTING READ FROM CONSOLE------------");
    printf("\nEnter a number: ");
    readline(buf,10);
    num = atoi(buf);
    printf("\nYour input was: %d ",num);
    printf("\n---------------FINISHED TESTING READ FROM CONSOLE------------");

    printf("\n\n\n---------------TESTING WRITE FROM CONSOLE------------");
    i=0;
    for(i=0;i<num;i++)
    {
        printf("\n iteration %d",i);
    }
    printf("\n---------------FINISHED TESTING WRITE FROM CONSOLE------------");

    printf("\n\n\n------------CHECKING INVALID READ CALLS--------------\n");
    num = read(3, &buf, 10);
    printf("Return on invalid file descriptor: %d\n", num);
    num = read(0, -12, 10);
    printf("Return on invalid vaddr: %d\n", num);
    num = read(0, &buf, -2);
    printf("Return on invalid size: %d\n", num);
    printf("------------END CHECKING INVALID READ CALLS--------------\n");

    printf("\n\n------------CHECKING INVALID WRITE CALLS--------------\n");
    num = write(3, &buf, 10);
    printf("Return on invalid file descriptor: %d\n", num);
    num = write(0, -12, 10);
    printf("Return on invalid vaddr: %d\n", num);
    num = write(0, &buf, -2);
    printf("Return on invalid size: %d\n", num);
    printf("------------END CHECKING INVALID WRITE CALLS--------------\n");

    printf("\n\n----------------TESTING EXEC---------------------\n\n");
    printf("forking and joining part2_test2.............");
    processID = exec("part2_test2.coff", 1,  execArgs);
    k = join(processID, &status1);
    printf("---------------- Join On Process %d Finished\nStatus Value:  %d    ---------------\n", processID, status1);
    printf("\n\n----------------FINISHED TESTING EXEC---------------------\n\n");


    printf("\n\n----------------TRYING TO HALT AS NON ROOT---------------------\n\n");
    printf("forking halt.coff and joining........ \n");
    processID = exec("halt.coff", 1,  execArgs);
    k = join(processID, &status1);
    printf("---------------- Join On Process %d Finished\nStatus Value:  %d    ---------------\n", processID, status1);
    printf("\n\n----------------FINISHED TRYING TO HALT AS NON ROOT---------------------\n\n");

    printf("\n\n\n------------CHECKING INVALID EXEC CALLS--------------\n");
    num = exec(-1, 1,  execArgs);
    printf("Return on invalid file pointer: %d\n", num);
    num = exec("nonExisting.coff", 1,  execArgs);
    printf("Return on invalid filename: %d\n", num);
    num = exec("part2_test2.coff", -1,  execArgs);
    printf("Return on invalid argument count: %d\n", num);
    num = exec("part2_test2.coff", 1,  -1);
    printf("Return on invalid argv: %d\n", num);
    printf("------------END CHECKING INVALID EXEC CALLS--------------\n");

    printf("\n\n\n------------CHECKING INVALID JOIN ON VALID EXEC CALLS--------------\n");
    int inval1,inval2,inval3;
    processID = exec("part2_test2.coff", 1,  execArgs);

    inval1 = join(-1, &status1);
    inval2 = join(40, &status1);
    inval3 = join(processID,-1);

    k = join(processID,&status1);
    printf("---------------- Join On Process %d Finished\nStatus Value:  %d    ---------------\n", processID, status1);

    printf("Return on invalid processID : %d\n", inval1);
    printf("Return on nonChild processID : %d\n",inval2);
    printf("Return on invalid status pointer: %d\n", inval3);

    printf("------------CHECKING INVALID JOIN ON VALID EXEC CALLS--------------\n");



    printf("\n\n----------------TESTING PARALLEL RUNNING PROCESS---------------------\n\n");

    printf("forking and joining letterPrinter and numberPrinter........ \n");
    processID1 = exec("letterPrinter.coff", 1,  execArgs);

    processID2 = exec("numberPrinter.coff", 1,  execArgs);


    l = join(processID2, &status2);
    k = join(processID1, &status1);

    printf("---------------- Join On Process %d Finished\nStatus Value:  %d    ---------------\n", processID1, status1);
    printf("---------------- Join On Process %d Finished\nStatus Value:  %d    ---------------\n", processID1, status2);

    printf("\n\n----------------FINISHED TESTING PARALLEL RUNNING PROCESS---------------------\n\n");





    printf("\n\n----------------TRYING TO HALT AS ROOT---------------------\n\n");
    halt();
    printf("\n\n----------------Shouldn't Reach Here---------------------\n\n");
    return 0;
}