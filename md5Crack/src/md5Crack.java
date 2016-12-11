/**
 * Created by Jose Rogado on 10-01-2016.
 */

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.jocl.CL.*;
import org.jocl.*;


public class md5Crack {
    // The OpenCL context
    private cl_context context;
    // The OpenCL command queue
    private cl_command_queue commandQueue;
    // The OpenCL kernel
    private cl_kernel kernel;
    // The OpenCL Program
    private cl_program cpProgram;
    // An OpenCL memory object which stores the searched MD5
    private cl_mem passHashMem;
    // An OpenCL memory object which stores the found pass string
    private cl_mem passMem;
    // An OpenCL memory object which stores the found status
    private cl_mem foundMem;
    // The global work size
    private static final int workSize = 512;

    // Possible password symbols
    String symbols;
    // The hash string to crack
    private static char[] passHash;
    private static int [] passHashInt;
    // Number of symbols in password
    private static final int PASSLEN = 8;
    // The found status
    private static int foundStatus[];
    // The found string
    private static int[] foundPass;

    public md5Crack() {
        symbols = "abcdefghijklmnopqrstuvwxyz*#";
        // MD5("helloo")
        // passHash = new char[]{0xb3, 0x73, 0x87, 0x0b, 0x91, 0x39, 0xbb, 0xad, 0xe8, 0x33, 0x96, 0xa4, 0x9b, 0x1a, 0xfc, 0x9a};
        // foundPass = new char[]{0x68, 0x65, 0x6c, 0x6c, 0x6f, 0x6f};

        // MD5("hellooxz") = 24 ac 24 cf ed 1d d3 6f e9 a3 b1 1f 1e a5 74 61
        passHash = new char[]{0x24, 0xac, 0x24, 0xcf, 0xed, 0x1d, 0xd3, 0x6f, 0xe9, 0xa3, 0xb1, 0x1f, 0x1e, 0xa5, 0x74, 0x61};
        // foundPass = new char[]{0x68, 0x65, 0x6c, 0x6c, 0x6f, 0x6f, 0x78, 0x7a};


        foundPass = new int[4*workSize]; // 2
        foundStatus = new int[workSize];
        passHashInt = chartoInt(passHash, true);

        initCL();
        // Allocate the memory object which contains the MD5 string
        // passHashMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
        //        passHash.length * Sizeof.cl_uchar, Pointer.to(passHash), null);
        passHashMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                  passHashInt.length * Sizeof.cl_uint, Pointer.to(passHashInt), null);
        clEnqueueWriteBuffer(commandQueue, passHashMem, true, 0,
                  passHashInt.length * Sizeof.cl_uint, Pointer.to(passHashInt), 0, null, null);

        // Create the memory object which will be filled with the found string
        passMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY,
                workSize*4*Sizeof.cl_uint, null, null); // 2

        // Create the memory object which will be filled with the result status
        foundMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY,
                workSize*Sizeof.cl_int, null, null);

    }

    /**
     * Entry point for this program.
     *
     */
    public static void main(String args[]) {

        // Initialize the CL context and memory objects
        final md5Crack md5 = new md5Crack();
        long startTime = System.currentTimeMillis();
        md5.compute();
        long stopTime = System.currentTimeMillis();
//        System.out.println(Sizeof.cl_mem + " " + Sizeof.cl_uchar + " " + Sizeof.cl_uint + " " + Sizeof.cl_uint4);
//        System.out.println(Sizeof.cl_uchar);
//        System.out.println(Sizeof.cl_uint);
//        System.out.println(Sizeof.cl_uint4);

        System.out.println("Pass Hash as bytes");
        for (int i = 0; i < passHash.length; i++)
            System.out.printf("%02x",(int)passHash[i]);
        System.out.println("\n");

        System.out.println("Pass Hash as integers");
        for (int i = 0; i < passHashInt.length; i++) {
            // System.out.printf(Integer.toHexString(passHashInt[i])+" ");
            System.out.printf("%08x ", passHashInt[i]);
        }
        System.out.println("\n");

        for (int i = 0; i < 4; i++)
            System.out.printf(Integer.toHexString(foundPass[i]) + " ");
        System.out.println();
        for (int i = 0; i < 4; i++)
            System.out.printf(Integer.toHexString(foundPass[4+i]) + " ");
        System.out.println();
        for (int i = 0; i < 4; i++)
            System.out.printf(Integer.toHexString(foundPass[8+i]) + " ");
        System.out.println();
        for (int i = 0; i < 4; i++)
            System.out.printf(Integer.toHexString(foundPass[12+i]) + " ");
        System.out.println("\n");

        for (int i = 0; i < 4; i++)
            System.out.println("Id: " + i + " status: " + foundStatus[i]);

        System.out.println("\nGenerating and comparing " + workSize + " MD5s took " + (stopTime - startTime) + " ms");

    }

    private void compute()
    {
        // Set the arguments for the kernel
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(passHashMem));
        clSetKernelArg(kernel, 1, Sizeof.cl_uint, Pointer.to(new int[]{workSize}));
        clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(foundMem));
        clSetKernelArg(kernel, 3, Sizeof.cl_mem, Pointer.to(passMem));

        // Set the work-item dimensions
        long global_work_size[] = new long[]{workSize};
        // Let OpenCL determine the best work-group size
        long local_work_size[] = new long[]{1};

        // Execute the kernel
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                global_work_size, local_work_size, 0, null, null);

        // Read the output status
        clEnqueueReadBuffer(commandQueue, foundMem, CL_TRUE, 0,
                workSize*Sizeof.cl_int, Pointer.to(foundStatus), 0, null, null);

        clEnqueueReadBuffer(commandQueue, passMem, CL_TRUE, 0,
                workSize*2*Sizeof.cl_uint, Pointer.to(foundPass), 0, null, null);

        clReleaseProgram(cpProgram);
        clReleaseMemObject(passHashMem);
        clReleaseMemObject(passMem);
        clReleaseMemObject(foundMem);
        clReleaseKernel(kernel);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);
    }
    /**
     * Initialize OpenCL: Create the context, the command queue
     * and the kernel.
     */
    private void initCL()
    {
        final int platformIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 0;

        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);

        // Obtain the number of platforms
        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];

        // Obtain a platform ID
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[platformIndex];

        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

        // Obtain the number of devices for the platform
        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];

        // Obtain a device ID
        cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
        cl_device_id device = devices[deviceIndex];

        // Create a context for the selected device
        context = clCreateContext(
                contextProperties, 1, new cl_device_id[]{device},
                null, null, null);

        // Create a command-queue for the selected device
        commandQueue =
                clCreateCommandQueue(context, device, 0, null);

        // Program Setup
        String source = readFile("D:\\Dropbox\\Ensino\\Projects\\IdeaProjects\\OpenCL\\md5Crack\\src\\md5Crack.cl");

        // Create the program
        cpProgram = clCreateProgramWithSource(context, 1,
                new String[]{ source }, null, null);

        // Build the program
        clBuildProgram(cpProgram, 0, null, "-cl-mad-enable", null, null);

        // Create the kernel
        kernel = clCreateKernel(cpProgram, "computeMD5", null);
    }

    /**
     * Helper function which reads the file with the given name and returns
     * the contents of this file as a String. Will exit the application
     * if the file can not be read.
     *
     * @param fileName The name of the file to read.
     * @return The contents of the file
     */
    private String readFile(String fileName)
    {
        try
        {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(fileName)));
            StringBuffer sb = new StringBuffer();
            String line = null;
            while (true)
            {
                line = br.readLine();
                if (line == null)
                {
                    break;
                }
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    // Convert chars to int (Little or Big Endian)
    private static int[] chartoInt(char[] pass, boolean LittleEndian) {
        if (pass.length > 16)
            return null;

        int[] value = new int[4];
        int intSize = Integer.BYTES;
        int add = ((pass.length % intSize) != 0) ? 1 : 0;
        int nints = pass.length/intSize + add;
        for (int i = 0; i < nints; i++) { // Big Endian
            if (LittleEndian)
                value[i] = pass[4*i+3] << 24 | pass[4*i+2]<< 16 | pass[4*i+1] << 8 | pass[4*i];
            else
                value[i] = pass[4*i] << 24 | pass[4*i+1]<< 16 | pass[4*i+2] << 8 | pass[4*i+3];
        }
        return value;
    }

/*
    private static int[] chartoInt(char[] pass) {
        if (pass.length > 16)
            return null;

        int[] value = new int[4];

        value[0] = pass[0] << 24 | pass[1]<< 16 | pass[2] << 8 | pass[3];
        value[1] = pass[4] << 24 | pass[5]<< 16 | pass[6] << 8 | pass[7];
        value[2] = pass[8] << 24 | pass[9]<< 16 | pass[10] << 8 | pass[11];
        value[3] = pass[12] << 24 | pass[13]<< 16 | pass[14] << 8 | pass[15];
        return value;

    }
*/
}

