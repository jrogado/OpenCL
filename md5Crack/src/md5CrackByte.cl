/*
 * md5Crack.cl
 *
 * Copyright 2016 Jose Rogado <Jose Rogado@JQR-VAYO>
 *
 */

/* Macros for reading/writing chars from int32's (from rar_kernel.cl) */
#define GETCHAR(buf, index) (((uchar*)(buf))[(index)])
#define PUTCHAR(buf, index, val) ((uchar*)(buf))[index] = (val)

/* The basic MD5 functions */
#ifdef USE_BITSELECT
#define F(x, y, z)	bitselect((z), (y), (x))
#define G(x, y, z)	bitselect((y), (x), (z))
#else
#define F(x, y, z)	((z) ^ ((x) & ((y) ^ (z))))
#define G(x, y, z)	((y) ^ ((z) & ((x) ^ (y))))
#endif
#define H(x, y, z)	((x) ^ (y) ^ (z))
#define I(x, y, z)	((y) ^ ((x) | ~(z)))

/* The MD5 transformation for all four rounds. */
#define STEP(f, a, b, c, d, x, t, s)	  \
	(a) += f((b), (c), (d)) + (x) + (t); \
	    (a) = (b) + ((a) << (s) | (a) >> (32 - (s)))
		// (a) = rotate((a), (uint)(s)); \
	    // (a) += (b)

#define ROUND_TAIL(a, b, expr, k, s, t)    \
	a += (expr) + UINT32_C(t) + block[k];  \
	a = b + (a << s | a >> (32 - s));

#define PASSLEN 4
#define NSYMBOLS 28

__kernel void computeMD5 (
   __global uchar *searchHash, // in
   int workSize,				// in
   __global int *foundStatus, // out
   __global uchar *foundPass	// out
   )
{
    uchar symbols[] = "abcdefghijklmnopqrstuvwxyz*#";
//    char *p = (char *)searchHash;

    uchar searchPass[PASSLEN+1];
    uint power[] = {1, 28, 784, 21952, 614656, 17210368};
    uint index = get_global_id(0);
    uint foundHash[4];
    uint diff;

    // Generate password for this index
    int i;
	int digit;

/*
	int nsymbols = NSYMBOLS;
    int debugIndex = 27;
    for (i = 0; i < PASSLEN+1; i++) {
        power[i] = (int) pow(nsymbols, i);
        if (index == debugIndex) {
            printf("%d %d\n", i, power[i]);
        }
	}
*/

    for (i = PASSLEN-1; i >= 0 ; i--) {
        digit = (int)(index % power[i+1])/power[i];
        searchPass[PASSLEN - 1 - i] = symbols[digit];
/*
        if (index == debugIndex)
       		printf("i: %d digit: %d pass[i]: %c symbol: %c\n", i, digit, searchPass[i], symbols[digit]);
*/
	}
	searchPass[PASSLEN] = 0;
/*
    if (index == debugIndex) {
        printf("GPU %d Received: ", index);
        for (i = 0; i < 16; i++)
    	    printf("%02x", searchHash[i]);
        printf("\nWorksize: %d\n", workSize);
        for (i = 0; i < PASSLEN; i++)
            printf("%c", searchPass[i]);
        printf(" \n");
    }
*/
    uint key[] = {0x6c6c6568, 0x7a786f6f};
    uint len = PASSLEN;
    //uint key[] = {0x6c6c6568, 0x6f6f};
    //uint len = 6;


    uint W[16] = { 0 };
    uint a, b, c, d;

//	for (i = 0; i < (len+3)/4; i++)
//		W[i] = key[i];

	for (i = 0; i < len; i++) {
		PUTCHAR(W, i, searchPass[i]);
	}

    PUTCHAR(W, len, 0x80);
    W[14] = len << 3;

	a = 0x67452301;
	b = 0xefcdab89;
	c = 0x98badcfe;
	d = 0x10325476;

	/* Round 1 */
	STEP(F, a, b, c, d, W[0], 0xd76aa478, 7);
	STEP(F, d, a, b, c, W[1], 0xe8c7b756, 12);
	STEP(F, c, d, a, b, W[2], 0x242070db, 17);
	STEP(F, b, c, d, a, W[3], 0xc1bdceee, 22);
	STEP(F, a, b, c, d, W[4], 0xf57c0faf, 7);
	STEP(F, d, a, b, c, W[5], 0x4787c62a, 12);
	STEP(F, c, d, a, b, W[6], 0xa8304613, 17);
	STEP(F, b, c, d, a, W[7], 0xfd469501, 22);
	STEP(F, a, b, c, d, W[8], 0x698098d8, 7);
	STEP(F, d, a, b, c, W[9], 0x8b44f7af, 12);
	STEP(F, c, d, a, b, W[10], 0xffff5bb1, 17);
	STEP(F, b, c, d, a, W[11], 0x895cd7be, 22);
	STEP(F, a, b, c, d, W[12], 0x6b901122, 7);
	STEP(F, d, a, b, c, W[13], 0xfd987193, 12);
	STEP(F, c, d, a, b, W[14], 0xa679438e, 17);
	STEP(F, b, c, d, a, W[15], 0x49b40821, 22);

	/* Round 2 */
	STEP(G, a, b, c, d, W[1], 0xf61e2562, 5);
	STEP(G, d, a, b, c, W[6], 0xc040b340, 9);
	STEP(G, c, d, a, b, W[11], 0x265e5a51, 14);
	STEP(G, b, c, d, a, W[0], 0xe9b6c7aa, 20);
	STEP(G, a, b, c, d, W[5], 0xd62f105d, 5);
	STEP(G, d, a, b, c, W[10], 0x02441453, 9);
	STEP(G, c, d, a, b, W[15], 0xd8a1e681, 14);
	STEP(G, b, c, d, a, W[4], 0xe7d3fbc8, 20);
	STEP(G, a, b, c, d, W[9], 0x21e1cde6, 5);
	STEP(G, d, a, b, c, W[14], 0xc33707d6, 9);
	STEP(G, c, d, a, b, W[3], 0xf4d50d87, 14);
	STEP(G, b, c, d, a, W[8], 0x455a14ed, 20);
	STEP(G, a, b, c, d, W[13], 0xa9e3e905, 5);
	STEP(G, d, a, b, c, W[2], 0xfcefa3f8, 9);
	STEP(G, c, d, a, b, W[7], 0x676f02d9, 14);
	STEP(G, b, c, d, a, W[12], 0x8d2a4c8a, 20);

	/* Round 3 */
	STEP(H, a, b, c, d, W[5], 0xfffa3942, 4);
	STEP(H, d, a, b, c, W[8], 0x8771f681, 11);
	STEP(H, c, d, a, b, W[11], 0x6d9d6122, 16);
	STEP(H, b, c, d, a, W[14], 0xfde5380c, 23);
	STEP(H, a, b, c, d, W[1], 0xa4beea44, 4);
	STEP(H, d, a, b, c, W[4], 0x4bdecfa9, 11);
	STEP(H, c, d, a, b, W[7], 0xf6bb4b60, 16);
	STEP(H, b, c, d, a, W[10], 0xbebfbc70, 23);
	STEP(H, a, b, c, d, W[13], 0x289b7ec6, 4);
	STEP(H, d, a, b, c, W[0], 0xeaa127fa, 11);
	STEP(H, c, d, a, b, W[3], 0xd4ef3085, 16);
	STEP(H, b, c, d, a, W[6], 0x04881d05, 23);
	STEP(H, a, b, c, d, W[9], 0xd9d4d039, 4);
	STEP(H, d, a, b, c, W[12], 0xe6db99e5, 11);
	STEP(H, c, d, a, b, W[15], 0x1fa27cf8, 16);
	STEP(H, b, c, d, a, W[2], 0xc4ac5665, 23);

	/* Round 4 */
	STEP(I, a, b, c, d, W[0], 0xf4292244, 6);
	STEP(I, d, a, b, c, W[7], 0x432aff97, 10);
	STEP(I, c, d, a, b, W[14], 0xab9423a7, 15);
	STEP(I, b, c, d, a, W[5], 0xfc93a039, 21);
	STEP(I, a, b, c, d, W[12], 0x655b59c3, 6);
	STEP(I, d, a, b, c, W[3], 0x8f0ccc92, 10);
	STEP(I, c, d, a, b, W[10], 0xffeff47d, 15);
	STEP(I, b, c, d, a, W[1], 0x85845dd1, 21);
	STEP(I, a, b, c, d, W[8], 0x6fa87e4f, 6);
	STEP(I, d, a, b, c, W[15], 0xfe2ce6e0, 10);
	STEP(I, c, d, a, b, W[6], 0xa3014314, 15);
	STEP(I, b, c, d, a, W[13], 0x4e0811a1, 21);
	STEP(I, a, b, c, d, W[4], 0xf7537e82, 6);
	STEP(I, d, a, b, c, W[11], 0xbd3af235, 10);
	STEP(I, c, d, a, b, W[2], 0x2ad7d2bb, 15);
	STEP(I, b, c, d, a, W[9], 0xeb86d391, 21);

	foundHash[0] = a + 0x67452301;
	foundHash[1] = b + 0xefcdab89;
	foundHash[2] = c + 0x98badcfe;
	foundHash[3] = d + 0x10325476;

/*    if (index == debugIndex) {
        printf(" GPU %d found: ", index);
        for (i = 0; i < 4; i++)
            printf("%x ", foundHash[i]);
        printf("\n");
    }
*/
	// Compare hashes
	for (i = 0; i < 16; i++) {
    	diff = searchHash[i] ^ GETCHAR(foundHash, i); // GETCHAR(buf, index)
	    if (diff)
	      break;
	}
	if (diff == 0) {
        foundStatus[index] = index;
        for (i = 0; i < PASSLEN+1; i++)
            foundPass[i] = searchPass[i];
    } else {
        foundStatus[index] = -1;
    }
}