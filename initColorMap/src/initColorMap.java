import java.awt.*;
import java.io.*;

/**
 * Created by Jose Rogado on 29-12-2015.
 */
public class initColorMap {
    private static int colorMap[];

    public static void main(String args[])
    {
        initColorMap.init(32, Color.RED, Color.GREEN, Color.BLUE);
        System.out.println("Generated color map of size " + colorMap.length);
        for (int i = 0; i < colorMap.length; i++) {
            int r = colorMap[i];
            if (i%4 == 0)
                System.out.println();
            System.out.print(String.format("{0x%x,0x%x,0x%x},", r>>16, (r>>8)&0xFF, r&0xFF));
        }
    }

    public static void init(int stepSize, Color... colors)
    {
        colorMap = new int[stepSize*colors.length];

        int index = 0;
        for (int i=0; i<colors.length-1; i++)
        {
            Color c0 = colors[i];
            int r0 = c0.getRed();
            int g0 = c0.getGreen();
            int b0 = c0.getBlue();

            Color c1 = colors[i+1];
            int r1 = c1.getRed();
            int g1 = c1.getGreen();
            int b1 = c1.getBlue();

            int dr = r1-r0;
            int dg = g1-g0;
            int db = b1-b0;

            for (int j=0; j<stepSize; j++)
            {
                float alpha = (float)j / (stepSize-1);
                int r = (int)(r0 + alpha * dr);
                int g = (int)(g0 + alpha * dg);
                int b = (int)(b0 + alpha * db);
                int rgb =
                        (r << 16) |
                                (g <<  8) |
                                (b <<  0);
                colorMap[index++] = rgb;
            }
        }
    }
}
