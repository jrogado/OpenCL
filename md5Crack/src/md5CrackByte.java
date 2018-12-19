/**
 * Created by Jose Rogado on 10-01-2016.
 */

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.jocl.CL.*;
import org.jocl.*;


public class md5CrackByte {
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
    private cl_mem statusMem;
    // The global work size
    private static int workSize;

    // Possible password symbols
    private static String symbols = "abcdefghijklmnopqrstuvwxyz*#";

    // The hash string to crack
    private static byte[] passHash;
    private static int nPasswords;

    // Number of symbols in password
    private static final int PASSLEN = 5;
    // The found status
    private static int[] foundStatus;
    // The found string
    private static byte[] foundPass;
    // The found hash
    // private static byte []foundHash;

    private md5CrackByte(String passHashString) {
        int i;

        nPasswords = (int) Math.pow(symbols.length(), PASSLEN);
        workSize = nPasswords;
        // Convert the 32 digits hash string to crack into bytes
        // Each byte corresponds to 2 string characters: we need 16 bytes
        passHash = new byte[passHashString.length() / 2];

        // Convert passHashString to bytes
        for (i = 0; i < passHashString.length() / 2; i++) {
            byte high = (byte)(xtob(passHashString.charAt(2 * i)) << 4);
            byte low = xtob(passHashString.charAt(2 * i + 1));
            passHash[i] = (byte) (high + low);
            // System.out.printf("%02x ", passHash[i]);
        }
        // System.out.printf("passHash size: %d\n", passHash.length);
        foundPass = new byte[(PASSLEN+1)];
        foundStatus = new int[]{-1, -1};
//        foundHash = new byte[passHash.length*workSize];

        initCL();
        // Allocate the memory object which contains the MD5 string
        passHashMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                passHash.length * Sizeof.cl_uchar, Pointer.to(passHash), null);
        clEnqueueWriteBuffer(commandQueue, passHashMem, true, 0,
                passHash.length * Sizeof.cl_uchar, Pointer.to(passHash),
                0, null, null);

        // Create the memory object which will be filled with the result status, previously filled with not found value (-1)
        statusMem = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR,
                2*Sizeof.cl_int, Pointer.to(foundStatus), null);
        clEnqueueWriteBuffer(commandQueue, statusMem, true, 0,
                2*Sizeof.cl_int, Pointer.to(foundStatus),
                0, null, null);

        // Create the memory object which will be filled with the found string
        passMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY,(PASSLEN+1)*Sizeof.cl_uchar, null, null); // 2

    }

    private void compute()
    {
        // Set the arguments for the kernel
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(passHashMem));
        clSetKernelArg(kernel, 1, Sizeof.cl_uint, Pointer.to(new int[]{workSize}));
        clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(statusMem));
        clSetKernelArg(kernel, 3, Sizeof.cl_mem, Pointer.to(passMem));

        // Set the work-item dimensions
        long[] global_work_size = new long[]{workSize};
        // Let OpenCL determine the best work-group size
        long[] local_work_size = new long[]{1};

        // Execute the kernel
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                global_work_size, local_work_size, 0, null, null);

        // Read the output status
        clEnqueueReadBuffer(commandQueue, statusMem, CL_TRUE, 0,
                2*Sizeof.cl_int, Pointer.to(foundStatus), 0, null, null);

        clEnqueueReadBuffer(commandQueue, passMem, CL_TRUE, 0,
                (PASSLEN+1)*Sizeof.cl_uchar, Pointer.to(foundPass), 0, null, null);

        clReleaseProgram(cpProgram);
        clReleaseMemObject(passHashMem);
        clReleaseMemObject(passMem);
        clReleaseMemObject(statusMem);
        clReleaseKernel(kernel);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);
    }

    /**
     * Entry point for this program.
     *
     */
    public static void main(String[] args) {
        String passHashString = "4f004a55c2cfd7a24efe33bae5711b11"; // 0b4e7a0e5fe84ad35fb5f95b9ceeac79

        // Initialize the CL context and memory objects
        final md5CrackByte md5 = new md5CrackByte(passHashString);
        System.out.printf("\nSearching %d passwords of %d characters using %d symbols: %s\n", nPasswords, PASSLEN, symbols.length(), symbols);
        System.out.print("Password Hash: ");
        printHash(passHash, passHash.length,"%02x",true);

        // Launch computation
        long startTime = System.currentTimeMillis();
        md5.compute();
        long stopTime = System.currentTimeMillis();
/*
        passHashInt = bytetoInt(passHash, true);
        System.out.println("Pass Hash as integers");
        for (int i = 0; i < passHashInt.length; i++) {
            // System.out.printf(Integer.toHexString(passHashInt[i])+" ");
            System.out.printf("%08x ", passHashInt[i]);
        }
        System.out.println("\n");
*/
        int i;
        if (foundStatus[0] >= 0) {
            System.out.printf("\nDone! Node: %d found password: ", foundStatus[0]);
            printHash(foundPass, PASSLEN, "%c",true);
        } else {
            System.out.println("\nPassword hash not found");
        }
        System.out.println("\nGenerating and comparing " + workSize + " MD5s took " + (stopTime - startTime) + " ms");
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
        int[] numPlatformsArray = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];

        // Obtain a platform ID
        cl_platform_id[] platforms = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[platformIndex];

        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

        // Obtain the number of devices for the platform
        int[] numDevicesArray = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];

        // Obtain a device ID
        cl_device_id[] devices = new cl_device_id[numDevices];
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
        String source = readFile("D:\\Dropbox\\Ensino\\Projects\\IdeaProjects\\OpenCL\\md5Crack\\src\\md5CrackByte.cl");

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
            StringBuilder sb = new StringBuilder();
            String line;
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

    // Prints hash bytes in given format
    private static void printHash(byte[] hash, int length, String format, boolean newline) {
        for (int i = 0; i < length; i++)
            System.out.printf(format, hash[i]);
        if (newline)
            System.out.println();
    }

    // Converts ascii hexadecimal to byte
    private static byte xtob(char c) {
        byte result;

        switch (c) {
            case 'f':
                result = 15;
                break;
            case 'e':
                result = 14;
                break;
            case 'd':
                result = 13;
                break;
            case 'c':
                result = 12;
                break;
            case 'b':
                result = 11;
                break;
            case 'a':
                result = 10;
                break;
            case '9':
                result = 9;
                break;
            case '8':
                result = 8;
                break;
            case '7':
                result = 7;
                break;
            case '6':
                result = 6;
                break;
            case '5':
                result = 5;
                break;
            case '4':
                result = 4;
                break;
            case '3':
                result = 3;
                break;
            case '2':
                result = 2;
                break;
            case '1':
                result = 1;
                break;
            case '0':
                result = 0;
            default:
                result = 0;
        }
        return result;
    }

    // Convert bytes to int (Little or Big Endian)
    private static int[] bytetoInt(byte[] pass, boolean LittleEndian) {
        if (pass.length > 16)
            return null;
        int[] value = new int[4];
        int intSize = Integer.BYTES;
        int add = ((pass.length % intSize) != 0) ? 1 : 0;
        int nInts = pass.length/intSize + add;
        // System.out.printf("\npass.length: %d nInts: %d\n", pass.length, nInts);
        for (int i = 0; i < nInts; i++) {
            if (LittleEndian) {
                value[i] = (pass[4*i+3] << 24) & 0xff000000 |
                        (pass[4*i+2] << 16) & 0x00ff0000 |
                        (pass[4*i+1] <<  8) & 0x0000ff00 |
                        (pass[4*i]) & 0x000000ff;
            } else // Big Endian
                value[i] = (pass[4*i] << 24) & 0xff000000 |
                        (pass[4*i+1] << 16) & 0x00ff0000 |
                        (pass[4*i+2] <<  8) & 0x0000ff00 |
                        (pass[4*i+3]) & 0x000000ff;
            // value[i] = (pass[4*i]&0x000000ff) << 24 | (pass[4*i+1]&0x00ff0000)<< 16 | (pass[4*i+2]&0x0000ff00) << 8 | (pass[4*i+3]&0x000000ff);
        }
        return value;
    }

}
