#include<stdio.h>
int main(){
	int x[10];
	int i = 0;
	for(i = 0;i<10;i++){
		x[i] = i + 1;
	}
	for(i = 0;i<10;i++){
		printf("%d\n", x[i]);
	}
	return 0;
}
