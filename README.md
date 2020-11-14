## CheetahTraj


#### Overview

The source code of CheetahTraj.

#### Prerequisites
* JDK 1.8
* Maven
* Dependent jars are in *lib* directory.

#### Files

Calculate VQGS:

* src.main.java.vqgs.vfgs.PreProcess.java: Pre-process for the original dataset.

* src.main.java.vqgs.vfgs.VFGS.java: The VQGS algorithm mentioned in the paper. Store all the result into static files. 

* src.main.java.vqgs.app.VFGSColor.java: The VQGS+ algorithm with color attribute mentioned in the paper.

* src.main.java.vqgs.app.ScreenShot.java: get the screenshot of the result, the figures are all in the *data/figure_result*. 

* src.main.java.vqgs.util.PSC: All the config information need, including the default file path.

Others:

* src.main.java.app.TimeProfile.java: run and speed up the render with GPU.

* src.main.java.index.SearchRegionPart.java: Build up the index and store the result into a file.

#### Datasets

We use two datasets in our work:
* GPS data for taxis in Portugal.
* GPS data for taxis in Shenzhen, China.
* GPS data for taxis in Chengdu, China.


The paths of the data(part) are *data/porto_5k* and *data/shenzhen_5k*, if you need the full dataset, please contect with us.

#### Run the program

Parameter usage:
>  --color, -c              :Choose the render type: color or not, not color by
                           default<br>
>   --dataset, -s DATASET    :The dataset path<br>
>   --datatype, -t DATATYPE  :The data type, including Shenzhen data(0) and Portugal
                           dataset(1)<br>
   --delta, -d DELTA        :The delta in VQGS+, 64 by default<br>
   --help, -h               :Show the help<br>
   --rate, -r RATE          :The sampling rate in VQGS/VQGS+, 0.005 by default<br>
   --vfgs, -g VQGS          :VQGS/VQGS+ calculation result directory path<br>
>
* Calculate the VQGS, VQGS+ and VQGS+ with color data:

File: src.main.java.vqgs.StaticCal.java
>javac StaticCal.java <br> 
>java StaticCal  -s &lt;dataset file path&gt;  -d &lt;delta>  -r &lt;rate&gt;


* Run the demo:

File: src.main.java.vqgs.app.VFGSMapApp.java
>javac VFGSMapApp.java <br> 
>java VFGSMapApp

The demo is coming with the data of Porto for total 5k trajectories, keyboard interrupt:

   >|  key   | function  |
   >|  ----  | ----  |
   >| p  | save the current screenshot  |
   >| o  | print current location |
   > |1 | show the full dataset|
   > | 2 | show the result of VQGS without color|                                                                                                          
   > |3 | show the result of VQGS with color|
   > |4| show the random result|                                                                                                          
   > |d| increase the delta value|
   > |a| decrease the delta value|
   > |s| increase the rate value|
   > |w| decrease the rate value|
   >|esc| quit|
 
 * Get all the screenshot:
 
 File: src.main.java.vqgs.app.ScreenShot.java
 >javac ScreenShot.java <br> 
 >java ScreenShot -s &lt;dataset file path&gt;  -g &lt;VQGS/VQGS+ file directory path&gt;
