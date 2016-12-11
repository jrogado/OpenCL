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

__kernel void computeMD5 (
   __global uint *searchHash, // in
   int workSize,				// in
   __global int *foundStatus, // out
   __global uint *foundPass	// out
   )
{
    //uchar pass[] = {0x68, 0x65, 0x6c, 0x6c, 0x6f, 0x6f, 0x78, 0x7a};
    //uchar pass[] = "hellooxz";

    uint index = get_global_id(0);
    uint foundHash[4];
    uint diff;

    uint key[] = {0x6c6c6568, 0x7a786f6f};
    uint len = 8;
    //uint key[] = {0x6c6c6568, 0x6f6f};
    //uint len = 6;


    uint W[16] = { 0 };
    uint i;
    uint a, b, c, d;

	for (i = 0; i < (len+3)/4; i++)
		W[i] = key[i];

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

	// Compare hashes
	for (i = 0; i < 4; i++) {
    	diff = searchHash[i] ^ foundHash[i];
	    if (diff)
	      break;
	}
	if (diff == 0)
        foundStatus[index] = get_global_size(0);
    else
        foundStatus[index] = -1;

    for (i = 0; i < 4; i++)
        foundPass[4*index+i] = foundHash[i];
/*
	foundPass[4*index] = foundHash[0];
	foundPass[4*index+1] = foundHash[1];
	foundPass[4*index+2] = foundHash[2];
	foundPass[4*index+3] = foundHash[3];

	foundStatus[index] = index;
*/
}

/*
        uchar pass[] = {0x68, 0x65, 0x6c, 0x6c, 0x6f, 0x6f, 0x78, 0x7a};
   		unsigned int index = get_global_id(0);

 		// if (index == 511)
 		//    printf("Message from index %d\n", index);
        //int value[2];

        // value[0] = pass[0] << 24 | pass[1]<< 16 | pass[2] << 8 | pass[3];
        // value[1] = pass[4] << 24 | pass[5]<< 16 | pass[6] << 8 | pass[7];
        //foundPass[2*index] = value[0];
        //foundPass[2*index+1] = value[1];

        //foundPass[4*index] = searchHash[0];
        //foundPass[4*index+1] = searchHash[1];
        //foundPass[4*index+2] = searchHash[2];
        //foundPass[4*index+3] = searchHash[3];

        for (i=0; i < 4; i++)
            foundPass[4*index+i] = searchHash[i];
*/