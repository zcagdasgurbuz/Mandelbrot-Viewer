 

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

public class MandelDraw extends JPanel {

    private final int prefWidth;
    private final int prefHeight;
    private double realStart;
    //private double realEnd;
    private double imgStart;
    //private double imgEnd;
    private double realRange;
    private double imgRange;
    private double midReal;
    private double midImg;
    private double grapedMidReal;
    private double grapedMidImg;
    private double zoom, zoomInc;
    private int pressedX;
    private int pressedY;

    private BufferedImage img;
    private double[] mandelReal;
    private double[] mandelImg;
    private int[] colorScheme;

    private int maxIterations ;
    private int infinity;
    private float hueShift;

    private final int widthCutOff;
    private final ForkJoinPool FJ_POOL = new ForkJoinPool();

    public MandelDraw(int prefWidth, int prefHeight) {

        setBorder(BorderFactory.createLineBorder(Color.black));
        this.prefWidth = prefWidth;
        this.prefHeight = prefHeight;
        //these stuff can be parameterized
        maxIterations = 1024;
        infinity = 2;
        hueShift = 0.3f;
        // these should be hard coded.
        zoom = 0.0;
        zoomInc = 0.05;
        realRange = 3.0;
        imgRange = 2.0;
        midReal = -0.5;
        midImg = 0.0;
        widthCutOff = 32;
        //set color scheme and start point of axes based on middle points and ranges
        setColorScheme();
        setStartsEnds();
        // here is the mouse listeners
        MouseControls mouseControl = new MouseControls();
        addMouseWheelListener(mouseControl);
        addMouseListener(mouseControl);
        addMouseMotionListener(mouseControl);
    }

    public static void createAndShowGUI() {
        //System.out.println("Created GUI on EDT? "+
        //    SwingUtilities.isEventDispatchThread());
        JFrame f = new JFrame("Mandelbrot");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setResizable(false);
        f.add(new MandelDraw(900, 600));
        f.pack();
        f.setVisible(true);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(prefWidth, prefHeight);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        long start = System.currentTimeMillis();
        setImageWithMandel(getWidth(), getHeight());
        g.drawImage(img, 0, 0, getWidth(), getHeight(), null);
        long end = System.currentTimeMillis();

        g.setColor(new Color(21, 0, 3));
        g.drawString("Done in " + (end - start) + "ms", 50, 50);
        g.drawString("Mid X  -> " + midReal + "  Mid Y -> " + midImg, 50, 75);
        g.drawString("Zoom ->  " + zoom, 50, 100);
    }

    private void setColorScheme(){
        colorScheme = new int[maxIterations + 1];
        for(int idx = 1; idx < maxIterations; idx++){
            colorScheme[idx] = Color.HSBtoRGB(((float) Math.log1p(idx / 20.0) * maxIterations / 360.0f) - hueShift,
                    0.6f + 0.2f / maxIterations * idx, 0.7f + 0.2f / maxIterations * idx);
        }
        colorScheme[maxIterations] = Color.HSBtoRGB(0.0f,0.0f, 0.0f);
    }

    private void setStartsEnds() {
        realStart = midReal - (realRange / 2.0);
        //realEnd = midReal + (realRange / 2.0);
        imgStart = midImg - (imgRange / 2.0);
        //imgEnd = midImg + (imgRange / 2.0);
    }

    private void setImageWithMandel(int width, int height) {
        img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);

        //do again
        setStartsEnds();

        //find increments
        double realInc = realRange / (double) (width - 1);
        double imgInc = imgRange / (double) (height - 1);

        //create real and imaginary axis value arrays
        mandelReal = new double[width];
        mandelImg = new double[height];

        // fill the arrays with values
        for (int realIdx = 0; realIdx < width; realIdx++) {
            mandelReal[realIdx] = realStart + (realInc * realIdx);
        }
        for (int imgIdx = 0; imgIdx < height; imgIdx++) {
            mandelImg[imgIdx] = imgStart + (imgInc * imgIdx);
        }

        //multi tread calculations starts
        FJ_POOL.invoke(new RecursiveFilling(0,0,getWidth(),getHeight()));

    }

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

    private class RecursiveFilling extends RecursiveAction {
        int rIdx;
        int iIdx;
        int w;
        int h;

        private RecursiveFilling(int realIdx, int imgIdx, int width, int height) {
            rIdx = realIdx;
            iIdx = imgIdx;
            w = width;
            h = height;
        }

        @Override
        public void compute() {
            //base case, if width is smaller than cutOff value
            if (w < widthCutOff) {
                calcIterationsSetPixels(rIdx, iIdx, w, h);
            } else {
                //divide the passed dimensions four equal dimensions and create recursive tasks for each of them
                RecursiveFilling upperLeft = new RecursiveFilling(rIdx, iIdx, (int)Math.round(w / 2.0), (int)Math.round(h / 2.0));
                RecursiveFilling upperRight = new RecursiveFilling(rIdx + w / 2, iIdx, (int)Math.round(w / 2.0), (int)Math.round(h / 2.0));
                RecursiveFilling lowerLeft = new RecursiveFilling(rIdx, iIdx + h / 2, (int)Math.round(w / 2.0), (int)Math.round(h / 2.0));
                RecursiveFilling lowerRight = new RecursiveFilling(rIdx + w / 2, iIdx + h / 2, (int)Math.round(w / 2.0), (int)Math.round(h / 2.0));
                // starts four recursive tasks and joins them
                invokeAll(upperLeft, upperRight, lowerLeft, lowerRight);
            }
        }
    }


    private class MouseControls extends MouseAdapter implements MouseListener {

        public void mouseWheelMoved(MouseWheelEvent e) {
            int wheelPos = e.getWheelRotation();
            zoom =  3.0 / realRange;
            realRange += wheelPos * realRange * zoomInc;
            imgRange += wheelPos * imgRange * zoomInc;
            repaint();
        }

        public void mouseDragged(MouseEvent e) {
            int relX = (e.getX() - pressedX);
            int relY = (e.getY() - pressedY);
            double realInc = realRange / (double) getWidth();
            double imgInc = imgRange / (double) getHeight();
            midReal = grapedMidReal - relX * realInc / 2.0;
            midImg = grapedMidImg - relY * imgInc / 2.0;
            repaint();
        }

        public void mousePressed(MouseEvent e) {
            pressedX = e.getX();
            pressedY = e.getY();
            grapedMidReal = midReal;
            grapedMidImg = midImg;
        }

        public void mouseReleased(MouseEvent e) {
            //nothing yet
        }

        public void mouseEntered(MouseEvent e) {
            //nothing yet
        }

        public void mouseExited(MouseEvent e) {
            //nothing yet
        }

        public void mouseClicked(MouseEvent e) {
            //nothing yet
        }

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MandelDraw::createAndShowGUI);
    }
}

