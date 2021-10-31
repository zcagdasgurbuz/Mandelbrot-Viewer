import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;


/**
 * This application draws the Mandelbrot fractal, also allows user to freely move and zoom on the fractal.
 * The zoom is limited to 2e14 for now because the application only uses doubles to calculate iterations.
 */
public class MandelDraw extends JPanel {

    private final int PREF_WIDTH;
    private final int PREF_HEIGHT;
    private final int W_CUT_OFF;
    private final ForkJoinPool FJ_POOL = new ForkJoinPool();

    private BufferedImage img;
    private double realStart;
    private double imgStart;
    private double realRange;
    private double imgRange;
    private double midReal;
    private double midImg;
    private double grapedMidReal;
    private double grapedMidImg;
    private double zoom, zoomInc;
    private int pressedX;
    private int pressedY;
    private double[] mandelReal;
    private double[] mandelImg;
    private int[] colorScheme;
    //TODO make these changeable by user
    private int maxIterations;
    private int infinity;
    private float hueShift;

    /**
     * The constructor of the application / object to be added to JPanel
     * <p>
     * Important note: Even though, the window size is parameterized, the fractal would not be in correct shape if the
     * width and height ratio is not 3/2. So, for now, always use 3/2, width/height ratio.(eg. 1200/800, 900/600)
     *
     * @param prefWidth  The preferred window width in pixels, negative value cause unexpected window sizes.
     * @param prefHeight The preferred window height in pixels, negative value cause unexpected window sizes.
     */
    public MandelDraw(int prefWidth, int prefHeight) {

        setBorder(BorderFactory.createLineBorder(Color.black));
        this.PREF_WIDTH = prefWidth;
        this.PREF_HEIGHT = prefHeight;
        //these stuff can be parameterized
        maxIterations = 1024;
        infinity = 2;
        hueShift = 0.3f;
        // these should be hard coded. Still need them here, development continues.
        zoom = 0.0;
        zoomInc = 0.05;
        realRange = 3.0;
        imgRange = 2.0;
        midReal = -0.5;
        midImg = 0.0;
        W_CUT_OFF = PREF_WIDTH / 16;
        //set color scheme and start point of axes based on middle points and ranges
        setColorScheme();
        setStarts();
        // here is the mouse listeners
        MouseControls mouseControl = new MouseControls();
        addMouseWheelListener(mouseControl);
        addMouseListener(mouseControl);
        addMouseMotionListener(mouseControl);
    }

    /**
     * The main method application starts here by invoking "createAndShowGUI" method.
     *
     * @param args Command line arguments, not used
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(MandelDraw::createAndShowGUI);
    }
    
    
    /**
     * This method is the start point of application, will be called from main method.
     * Also, this is taken from Java Api example for JPanel.
     */
    public static void createAndShowGUI() {

        JFrame f = new JFrame("Mandelbrot");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setResizable(false);
        f.add(new MandelDraw(1050, 700));
        f.pack();
        f.setVisible(true);
    }

    /**
     * {inheritDoc}
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(PREF_WIDTH, PREF_HEIGHT);
    }

    /**
     * {inheritDoc}
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        long start = System.currentTimeMillis();
        setImageWithMandel();
        g.drawImage(img, 0, 0, getWidth(), getHeight(), null);
        long end = System.currentTimeMillis();

        g.setColor(new Color(21, 0, 3));
        g.drawString("Done in " + (end - start) + "ms", 50, 50);
        g.drawString("Mid X  -> " + midReal + "  Mid Y -> " + midImg, 50, 75);
        g.drawString("Zoom ->  " + zoom, 50, 100);
        g.drawString("Iteration ->  " + maxIterations, 50, 125);
    }

    /**
     * This method creates somewhat random color scheme for the iterations. Uses hue's 360 degree rotation to
     * create different colors for each iteration count. This method is used during the setting the pixels of the image.
     */
    private void setColorScheme() {
        colorScheme = new int[maxIterations + 1];
        for (int idx = 1; idx < maxIterations; idx++) {
            // math can be changed, found by try and error.
            colorScheme[idx] = Color.HSBtoRGB(((float) Math.log1p(idx / 20.0) * maxIterations / 360.0f) - hueShift,
                    0.6f + 0.2f / maxIterations * idx, 0.7f + 0.2f / maxIterations * idx);
        }
        colorScheme[maxIterations] = Color.HSBtoRGB(0.0f, 0.0f, 0.0f);
    }

    /**
     * This method updates the start points of visible real and imaginary axis by using the mid points and ranges.
     */
    private void setStarts() {
        realStart = midReal - (realRange / 2.0);
        imgStart = midImg - (imgRange / 2.0);
    }

    /**
     * This method creates arrays for real and imaginary axis numbers for the corresponding pixels. The numbers are
     * evenly distributed within their ranges.
     */
    private void setImageWithMandel() {
        img = new BufferedImage(PREF_WIDTH, PREF_HEIGHT, BufferedImage.TYPE_INT_RGB);

        //do again
        setStarts();

        //find increments
        double realInc = realRange / (double) (PREF_WIDTH - 1);
        double imgInc = imgRange / (double) (PREF_HEIGHT - 1);

        //create real and imaginary axis value arrays
        mandelReal = new double[PREF_WIDTH];
        mandelImg = new double[PREF_HEIGHT];

        // fill the arrays with values
        for (int realIdx = 0; realIdx < PREF_WIDTH; realIdx++) {
            mandelReal[realIdx] = realStart + (realInc * realIdx);
        }
        for (int imgIdx = 0; imgIdx < PREF_HEIGHT; imgIdx++) {
            mandelImg[imgIdx] = imgStart + (imgInc * imgIdx);
        }

        //multi tread calculations starts
        FJ_POOL.invoke(new RecursiveFilling(0, 0, getWidth(), getHeight()));

    }

    /**
     * This method calculates iteration counts for each pixel withing given range, then sets the buffered image
     * using the calculated color scheme.
     *
     * @param realIdx X-coordinate of the starting point of the calculation
     * @param imgIdx  Y-coordinate of the starting point of the calculation
     * @param width   X range of the calculation
     * @param height  Y range of the calculation
     */
    private void calcIterationsSetPixels(int realIdx, int imgIdx, int width, int height) {

        double zReal, zImg;
        double zRealSquare, zImgSquare, zRealTimesImg;
        int iteration;
        int infinitySquare = infinity * infinity;

        for (int iIdx = imgIdx; iIdx < imgIdx + height; iIdx++) {
            for (int rIdx = realIdx; rIdx < realIdx + width; rIdx++) {
                zRealSquare = 0.0;
                zImgSquare = 0.0;
                zRealTimesImg = 0.0;
                iteration = 0;
                //TODO figure out how to get more precision
                while (iteration < maxIterations && (zRealSquare + zImgSquare) < infinitySquare) {
                    //z = z^2 + c avoiding multiplications
                    zImg = zRealTimesImg + zRealTimesImg + mandelImg[iIdx];
                    zReal = (zRealSquare) - (zImgSquare) + mandelReal[rIdx];
                    zRealSquare = zReal * zReal;
                    zImgSquare = zImg * zImg;
                    zRealTimesImg = zReal * zImg;
                    iteration++;
                }
                img.setRGB(rIdx, iIdx, colorScheme[iteration]);
            }
        }
    }

    /**
     * This internal class is used to apply "calcIterationsSetPixels" method recursively
     */
    private class RecursiveFilling extends RecursiveAction {
        int rIdx;
        int iIdx;
        int w;
        int h;

        /**
         * The constructor of the recursive action class, passes the parameters to instance variables to use
         * them recursively.
         *
         * @param realIdx X-coordinate of the starting point of the calculation
         * @param imgIdx  Y-coordinate of the starting point of the calculation
         * @param width   X range of the calculation
         * @param height  Y range of the calculation
         */
        private RecursiveFilling(int realIdx, int imgIdx, int width, int height) {
            rIdx = realIdx;
            iIdx = imgIdx;
            w = width;
            h = height;
        }

        /**
         * Recursive action starts here, the passed window(start values and sizes) gets divided into four recursively 
         * until smallest piece has smaller width than the width cut of value. 
         * Then the pixel iteration values gets calculated.
         */
        @Override
        public void compute() {
            //base case, if width is smaller than cutOff value
            if (w < W_CUT_OFF) {
                calcIterationsSetPixels(rIdx, iIdx, w, h);
            } else {
                //divide the passed dimensions four equal dimensions and create recursive tasks for each of them
                RecursiveFilling upperLeft = new RecursiveFilling(rIdx, iIdx, 
                        (int) Math.round(w / 2.0), (int) Math.round(h / 2.0));
                RecursiveFilling upperRight = new RecursiveFilling(rIdx + w / 2, iIdx, 
                        (int) Math.round(w / 2.0), (int) Math.round(h / 2.0));
                RecursiveFilling lowerLeft = new RecursiveFilling(rIdx, iIdx + h / 2, 
                        (int) Math.round(w / 2.0), (int) Math.round(h / 2.0));
                RecursiveFilling lowerRight = new RecursiveFilling(rIdx + w / 2, iIdx + h / 2, 
                        (int) Math.round(w / 2.0), (int) Math.round(h / 2.0));
                // starts four recursive tasks and joins them
                invokeAll(upperLeft, upperRight, lowerLeft, lowerRight);
            }
        }
    }


    /**
     * This internal class makes mouse controls possible.
     */
    private class MouseControls extends MouseAdapter implements MouseListener {
        /**
         * Increases or decreases the real and imaginary ranges by mouse wheel rotation, also calculates the zoom value.
         *
         * @param e Mouse wheel event, passed by the mouse adapter.
         */
        public void mouseWheelMoved(MouseWheelEvent e) {
            int wheelPos = e.getWheelRotation();
            zoom = 3.0 / realRange;
            realRange += wheelPos * realRange * zoomInc;
            imgRange += wheelPos * imgRange * zoomInc;
            repaint();
        }

        /**
         * Changes the middle point of the image by dragging the mouse.
         *
         * @param e Mouse event, passed by the mouse adapter
         */
        public void mouseDragged(MouseEvent e) {
            int relX = (e.getX() - pressedX);
            int relY = (e.getY() - pressedY);
            double realInc = realRange / (double) getWidth();
            double imgInc = imgRange / (double) getHeight();
            midReal = grapedMidReal - relX * realInc / 2.0;
            midImg = grapedMidImg - relY * imgInc / 2.0;
            repaint();
        }

        /**
         * Saves the middle point coordinates and values to be used in mouseDragged method.
         *
         * @param e Mouse event, passed by the mouse adapter
         */
        public void mousePressed(MouseEvent e) {
            pressedX = e.getX();
            pressedY = e.getY();
            grapedMidReal = midReal;
            grapedMidImg = midImg;
        }

        /**
         * Not implemented
         *
         * @param e Mouse event, passed by the mouse adapter
         */
        public void mouseReleased(MouseEvent e) {
            //nothing yet
        }

        /**
         * Not implemented
         *
         * @param e Mouse event, passed by the mouse adapter
         */
        public void mouseEntered(MouseEvent e) {
            //nothing yet
        }

        /**
         * Not implemented
         *
         * @param e Mouse event, passed by the mouse adapter
         */
        public void mouseExited(MouseEvent e) {
            //nothing yet
        }

        /**
         * Not implemented
         *
         * @param e Mouse event, passed by the mouse adapter
         */
        public void mouseClicked(MouseEvent e) {
            //nothing yet
        }

    }
}

