package com.communex.dsa.sorting;

/** Four-pass stable LSD radix sort for all signed 32-bit integers. */
public final class RadixSort {
    private RadixSort() { }
    public static int[] sort(int[] input) {
        int[] source=BubbleSort.copy(input);
        if(source.length<2)return source;
        int[] target=new int[source.length];
        for(int shift=0;shift<32;shift+=8){
            int[] counts=new int[256];
            for(int value:source)
                counts[((value^Integer.MIN_VALUE)>>>shift)&0xff]++;
            int total=0;
            for(int i=0;i<counts.length;i++){
                int count=counts[i];
                counts[i]=total;
                total+=count;
            }
            for(int value:source)
                target[counts[((value^Integer.MIN_VALUE)>>>shift)&0xff]++]=value;
            int[] swap=source;
            source=target;
            target=swap;
        }
        return source;
    }
}
