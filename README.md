# Mandelbrot Viewer

This simple java swing Mandelbrot set viewer utilizes java concurrency tools - ForkJoinPool and RecursiveAction 
-for faster rendering. Also, it is possible to move around and zoom in /out by using mouse controls.  
-Drag to move.  
-Use mouse wheel to zoom in and out.

[Download JAR](https://github.com/zcagdasgurbuz/Mandelbrot-Viewer/releases/tag/v1.0.0)

![Demo](/img/mandel_demo.gif)



### What is Mandelbrot Set? 

The Mandelbrot set (/ˈmændəlbrɒt/) is the set of complex numbers "c" for which the function
<img src="https://render.githubusercontent.com/render/math?math=f_{c}(z)=z^{2} %2B c"> does not diverge to infinity when iterated from 
z = 0, i.e., for which the sequence <img src="https://render.githubusercontent.com/render/math?math=f_{c}(0)">,
<img src="https://render.githubusercontent.com/render/math?math=f_{c}(f_{c}(0))">}, etc., remains bounded in absolute value. 
Its definition is credited to Adrien Douady who named it in tribute to the mathematician Benoit Mandelbrot,
a pioneer of fractal geometry.

[Source](https://en.wikipedia.org/wiki/Mandelbrot_set)


### How does this viewer render the set?

Basically, every pixel is associated with a complex number in this simple viewer, as if the window is a complex plane. 
However, what is visible on the viewer screen is the arbitrary color representation of the result of the recursive function 
applied to each pixel separately. For example, for a pixel associated with the value 1+1i, the iterative function is like;  
1. <img src="https://render.githubusercontent.com/render/math?math=f_{c}(z)=(0 %2B 0i)^{2} %2B (1 %2B 1i) = (1 %2B 1i)" >
2. <img src="https://render.githubusercontent.com/render/math?math=f_{c}(z)=(1 %2B 1i)^{2} %2B (1 %2B 1i) = (1 %2B 3i)">
3. <img src="https://render.githubusercontent.com/render/math?math=f_{c}(z)=(1 %2B 3i)^{2} %2B (1 %2B 1i) = (-7 %2B 7i)">
4. <img src="https://render.githubusercontent.com/render/math?math=f_{c}(z)=(-7 %2B 7i)^{2} %2B (1 %2B 1i) = (1 - 97i)">
5. ...
6. ...

After each function calculation or loop, the result is checked if its magnitude -distance from the origin of the complex
plane- is greater than some arbitrary number that represents infinity. If so, the calculation loop stops, and the iteration 
count for that pixel defines its color. On the other hand, if the magnitude of the result doesn't reach the arbitrary infinity 
until the calculation loop stops at some arbitrary iteration limit, that pixel/number is considered in the Mandelbrot set and
drawn in black.



