// Copyright 2021 The MediaPipe Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.mediapipe.examples.hands

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import com.google.mediapipe.framework.TextureFrame
import com.google.mediapipe.solutioncore.CameraInput
import com.google.mediapipe.solutioncore.SolutionGlSurfaceView
import com.google.mediapipe.solutioncore.VideoInput
import com.google.mediapipe.solutions.hands.HandLandmark
import com.google.mediapipe.solutions.hands.Hands
import com.google.mediapipe.solutions.hands.HandsOptions
import com.google.mediapipe.solutions.hands.HandsResult
import java.io.IOException
import java.io.InputStream

// ContentResolver dependency
/** Main activity of MediaPipe Hands app.  */
class MainActivity : AppCompatActivity() {
    private var hands: Hands? = null

    private enum class InputSource {
        UNKNOWN, IMAGE, VIDEO, CAMERA
    }

    private var inputSource = InputSource.UNKNOWN

    // Image demo UI and image loader components.
    private var imageGetter: ActivityResultLauncher<Intent>? = null
    private var imageView: HandsResultImageView? = null

    // Video demo UI and video loader components.
    private var videoInput: VideoInput? = null
    private var videoGetter: ActivityResultLauncher<Intent>? = null

    // Live camera demo UI and camera components.
    private var cameraInput: CameraInput? = null
    private var glSurfaceView: SolutionGlSurfaceView<HandsResult>? = null
    private val landmarksLibrary: MutableList<DoubleArray> = ArrayList()
    private val landmarksLibraryNames: MutableList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.hide()
        setContentView(R.layout.activity_main)
        val textLayout = findViewById<TextView>(R.id.pars)
        setupStaticImageDemoUiComponents()
        setupVideoDemoUiComponents()
        setupLiveDemoUiComponents()
        landmarksLibrary.add(
            doubleArrayOf(-4.3563498E-4,0.077118635,-0.045234114,-0.029731305,0.057175975,-0.037000887,-0.05162954,0.037956763,-0.020224102,-0.07723952,0.01590538,0.0042629614,-0.09469522,-0.0016334532,0.025586352,-0.029367633,0.0019884373,0.008816563,-0.034597125,-0.022065196,0.009787098,-0.03980871,-0.036910456,0.0077486336,-0.04803985,-0.046135068,-0.012439176,-0.0040241634,-0.0020144186,0.00925784,-0.001843017,-0.037020586,0.005446486,-0.007394218,-0.05690279,-0.008651383,-0.014782369,-0.0735496,-0.021308884,0.019367907,-0.0042848927,-0.004987158,0.021432824,-0.032545026,-0.0041790158,0.019227978,-0.053764354,-0.013897598,0.011644704,-0.07077594,-0.023126386,0.031938184,0.0066931443,-0.02025297,0.0467552,-0.012884916,-0.014097586,0.05366315,-0.033438247,-0.01473555,0.053088218,-0.051110417,-0.019481525))
        landmarksLibraryNames.add("NoName")
        landmarksLibrary.add(
            doubleArrayOf(0.025962051,0.08413271,0.02520039,-0.0073120203,0.071851864,0.0037905872,-0.034311652,0.057118326,-0.0036999658,-0.061333355,0.040304285,-0.004727237,-0.08908754,0.028132787,0.0010085255,-0.02956133,0.0072105043,-3.684871E-4,-0.03369297,-0.021192316,-0.0037440658,-0.038881443,-0.041500714,-0.012955472,-0.042785197,-0.055715173,-0.039449185,-0.0055677113,-0.0020993613,0.0053770654,-0.006360585,-0.0150147835,-0.030523017,-0.0063530263,0.0111194495,-0.045312837,-0.009695799,0.030528126,-0.035606295,0.017822742,-0.0076981992,0.0018609762,0.016941762,-0.015308406,-0.029408216,0.013690893,0.0054441486,-0.04235661,0.009778412,0.02343952,-0.03863497,0.036558904,0.005695453,-0.0023866147,0.042022653,-0.018739434,-0.009848818,0.03976123,-0.03605947,-0.022168726,0.036374554,-0.050013304,-0.031076789))
        landmarksLibraryNames.add("Rock")
        landmarksLibrary.add(
            doubleArrayOf(-0.05505785,0.035954367,0.06346174,-0.055405542,0.006431829,0.039878488,-0.047092862,-0.017942939,0.028393358,-0.0418671,-0.044892758,0.007118568,-0.03432099,-0.07321368,-0.004568711,-0.008751581,-0.025197767,0.0045058057,-0.019302342,-0.020878237,-0.026211545,-0.03045405,-0.014768166,-0.025170196,-0.034725722,-0.013415413,-0.010488242,0.001485556,-0.0050625317,0.0025557652,-0.009722997,-0.0014624558,-0.03194741,-0.035334576,0.0024334798,-0.030021861,-0.028127495,0.0031424155,-0.0022605956,0.0068031847,0.01348031,-0.0020362288,-0.013062056,0.02339368,-0.030342825,-0.03200853,0.022966947,-0.02514869,-0.023796558,0.019989697,4.907325E-4,-0.001986798,0.0350241,-0.00280777,-0.014849359,0.04018319,-0.020293549,-0.030628566,0.037102707,-0.019141153,-0.029639358,0.03366862,-0.008482456,))
        landmarksLibraryNames.add("Up1")
        landmarksLibrary.add(
            doubleArrayOf(-0.007469258,0.02261421,0.09247559,-0.014565084,-0.0044757687,0.06807396,-0.010746501,-0.026848625,0.046822667,-0.013274053,-0.05217359,0.022388548,-0.0044427924,-0.0732562,-0.005824417,-0.003943707,-0.022711433,3.652349E-4,-0.030997016,-0.023847574,-0.015284851,-0.034257814,-0.025213435,0.004465604,-0.025937784,-0.02427467,0.027987361,-4.0917564E-4,-0.004114256,-0.0020478517,-0.024142373,-0.009832127,-0.018250339,-0.03454505,-0.011342453,0.0030820519,-0.01443228,-0.009675525,0.029154599,0.005209226,0.013145134,-0.001438342,-0.028690686,0.008864857,-0.009832345,-0.033889495,0.0072647976,0.011529848,-0.017766554,0.010447715,0.039479166,-0.006845317,0.028317504,0.0072260834,-0.028212687,0.025085088,0.0011559427,-0.0353667,0.017749712,0.015641123,-0.029971872,0.023532432,0.033807814))
        landmarksLibraryNames.add("Up2")
        landmarksLibrary.add(
            doubleArrayOf(-0.007469258,0.02261421,0.09247559,-0.014565084,-0.0044757687,0.06807396,-0.010746501,-0.026848625,0.046822667,-0.013274053,-0.05217359,0.022388548,-0.0044427924,-0.0732562,-0.005824417,-0.003943707,-0.022711433,3.652349E-4,-0.030997016,-0.023847574,-0.015284851,-0.034257814,-0.025213435,0.004465604,-0.025937784,-0.02427467,0.027987361,-4.0917564E-4,-0.004114256,-0.0020478517,-0.024142373,-0.009832127,-0.018250339,-0.03454505,-0.011342453,0.0030820519,-0.01443228,-0.009675525,0.029154599,0.005209226,0.013145134,-0.001438342,-0.028690686,0.008864857,-0.009832345,-0.033889495,0.0072647976,0.011529848,-0.017766554,0.010447715,0.039479166,-0.006845317,0.028317504,0.0072260834,-0.028212687,0.025085088,0.0011559427,-0.0353667,0.017749712,0.015641123,-0.029971872,0.023532432,0.033807814))
        landmarksLibraryNames.add("Up3")
        landmarksLibrary.add(
            doubleArrayOf(0.020372704,0.024596909,0.08076474,-4.7622737E-4,0.0039305896,0.06582853,-0.0054953517,-0.0135398,0.048169136,0.0013910839,-0.026471835,0.02497176,0.007084308,-0.053391613,0.0021805912,-0.003921859,-0.0011433152,0.003322795,-0.012663849,0.0038879644,0.0071350783,-0.015343923,3.564353E-4,0.035337918,-0.009000864,-5.138144E-4,0.056729376,8.260829E-4,3.9407448E-4,-0.003397528,-0.01062333,0.0038722756,7.075742E-4,-0.013423339,0.009528358,0.029366016,-0.012016502,0.0015850607,0.050640054,0.0031432183,1.1127675E-5,-0.002073124,-0.007921653,0.010137843,0.0010400265,-0.009498929,0.014350728,0.028170347,-0.0076927645,0.009736379,0.04707841,0.0021707797,0.012772773,0.008364286,-0.009084229,0.014727185,0.014036864,-0.009433135,0.017541986,0.033928543,-0.0039729653,0.018832337,0.049791187))
        landmarksLibraryNames.add("Up4")
        landmarksLibrary.add(
            doubleArrayOf(0.010696798,0.09638068,0.0016533136,-0.019983314,0.0731943,-0.003071636,-0.039571054,0.051399212,-0.0048184246,-0.05845803,0.025763843,-0.0076470524,-0.07694064,0.0046426896,-0.0033908784,-0.026905926,-5.6889374E-4,0.010350138,-0.027070783,-0.031585876,0.0013465881,-0.026690137,-0.05109292,-0.007949375,-0.028829776,-0.069671586,-0.030988961,-0.0036034936,-0.0046170084,0.006994091,-0.008358917,-0.04263335,-0.0017452724,-0.013232218,-0.06540494,-0.015898101,-0.018795231,-0.088878304,-0.028694965,0.01670111,-8.060441E-4,-0.0061864033,0.011679631,-0.031331256,-0.015856184,0.005312711,-0.053309664,-0.029382914,-0.0020135678,-0.07374519,-0.040899083,0.030402977,0.015237713,-0.017966539,0.031951454,-0.008325376,-0.022438206,0.025533054,-0.028271407,-0.033793256,0.01766248,-0.04549261,-0.047332734,))
        landmarksLibraryNames.add("Stop")
        landmarksLibrary.add(
            doubleArrayOf(0.01944191,0.09370463,0.0061322227,-0.011365933,0.07392706,-0.0029294565,-0.03124937,0.056569126,-0.014425285,-0.048655383,0.029050376,-0.028561722,-0.042785585,2.418859E-4,-0.03558585,-0.028272344,0.005391744,0.00491444,-0.033554908,-0.024332222,-0.0011933744,-0.039670553,-0.043718792,-0.012538411,-0.04398945,-0.05779596,-0.038822964,-0.0051048514,-0.0030670157,0.006535683,-0.013427715,-0.041777704,1.8649176E-4,-0.024595995,-0.06306004,-0.017415866,-0.034365416,-0.08368402,-0.037412494,0.01783449,-0.004732931,-0.002267234,0.018332276,-0.03432346,-0.0127064735,0.019065259,-0.055778172,-0.030599691,0.017383397,-0.073619835,-0.050753534,0.03282564,0.008944364,-0.011934459,0.038683377,-0.0138676,-0.013147175,0.035402186,-0.033003286,-0.02616942,0.030636482,-0.048501547,-0.044611067))
        landmarksLibraryNames.add("Vulkan")
        landmarksLibrary.add(
            doubleArrayOf(0.0016070269,0.09262243,-0.00989794,-0.025908977,0.067672916,-0.018417075,-0.034067452,0.03861931,-0.030553922,-0.03458475,0.007635969,-0.040357806,-0.023548255,-0.018533768,-0.04678741,-0.027589794,-0.0022767838,0.0034581646,-0.028519403,-0.030571558,3.3719093E-4,-0.028420318,-0.052045956,-0.005342398,-0.029625997,-0.07197826,-0.021741107,-0.0040656836,-0.0031627796,0.0071534403,-0.0018120744,-0.029990716,-0.019201681,-0.010892633,-0.010839836,-0.04053343,-0.014677956,0.009706608,-0.03976821,0.017499162,-7.560194E-4,-0.0012824163,0.017631238,-0.019540992,-0.025914378,0.0073016323,-0.0028648428,-0.042382583,0.0040222257,0.018198688,-0.041715242,0.030692136,0.013615714,-0.011969969,0.036971804,-0.0063036457,-0.024339251,0.025902962,-0.003707757,-0.04158868,0.017487662,0.013276218,-0.043687344))
        landmarksLibraryNames.add("One")
        landmarksLibrary.add(
            doubleArrayOf(0.0014286271,0.08431559,0.02714575,-0.026289305,0.061061203,0.0071926564,-0.044492662,0.04151895,0.004180044,-0.06909478,0.01781739,0.003953971,-0.092826314,-0.0020756728,0.014930159,-0.026591092,-0.0050466536,0.0014739893,-0.021202827,-0.03350315,-0.005987704,-0.022524169,-0.056079358,-0.018766236,-0.020154446,-0.0714702,-0.046310544,-0.004024186,-0.005331679,0.0057644174,-0.0022770544,-0.010822939,-0.031201422,-0.012664511,0.012911873,-0.04175976,-0.017568031,0.022689344,-0.023543924,0.017235594,0.0018819608,0.0012966767,0.014193727,0.0039720167,-0.03235309,0.0033261161,0.026859956,-0.035296246,-0.0030850817,0.03371761,-0.018275537,0.028622588,0.022666855,-0.004615318,0.031723045,0.013344067,-0.025789313,0.016844857,0.026792653,-0.0410749,0.010931149,0.035923358,-0.029164523))
        landmarksLibraryNames.add("Two")
        landmarksLibrary.add(
            doubleArrayOf(0.0072652176,0.094236456,0.0076112524,-0.020573381,0.06922686,-0.008629471,-0.023845473,0.04277982,-0.027168244,-0.01611951,0.011912479,-0.048197426,0.005216002,-0.011056874,-0.048855945,-0.02875998,-0.0014548083,4.7278404E-4,-0.03746941,-0.030603034,-0.004982069,-0.043476705,-0.055081893,-0.011867754,-0.047661364,-0.072113164,-0.0318978,-0.004708932,-0.0040650456,0.004951602,0.0015088087,-0.047110282,0.0025271997,-0.0013560168,-0.07203315,-0.010336772,-1.6382709E-4,-0.095386036,-0.027640566,0.018546732,-6.878509E-4,6.034449E-4,0.018966565,-0.016918622,-0.02564168,0.009733069,-9.785278E-5,-0.041907415,0.002281347,0.024740778,-0.044835284,0.033789456,0.017065104,-0.00553786,0.032394223,0.0062021213,-0.02323284,0.019058429,0.016716525,-0.036221385,0.014573816,0.032526944,-0.034053788))
        landmarksLibraryNames.add("Peace")
        landmarksLibrary.add(
            doubleArrayOf(0.0025351075,0.09386457,0.022159941,-0.024637893,0.068644986,0.005959213,-0.0415514,0.043150544,-0.0045066997,-0.056105845,0.014604883,-0.017260507,-0.0727232,-0.0065646563,-0.018148303,-0.029028965,-0.0032040593,0.0022684634,-0.034168947,-0.031567737,-0.005436316,-0.039417755,-0.050077453,-0.013738114,-0.042980887,-0.06706355,-0.037054062,-0.005015266,-0.004330768,0.004109621,-0.0011361979,-0.042900085,-0.0045610964,-0.0044705975,-0.06405832,-0.02159787,-0.0077761505,-0.08268104,-0.041826054,0.018326223,4.9587723E-4,-6.1450154E-4,0.023550019,-0.01737336,-0.02285909,0.013748873,-0.0034842528,-0.04042629,0.005798665,0.0177496,-0.04900989,0.034445304,0.018438376,-0.0051931515,0.037716042,0.0038771485,-0.022369243,0.02865159,0.010982623,-0.036981374,0.019729948,0.028490266,-0.040471554))
        landmarksLibraryNames.add("ThreeE")
        landmarksLibrary.add(
            doubleArrayOf(0.01571872,0.10134019,0.012605108,-0.014623314,0.08209733,-0.0044303834,-0.01710107,0.052818123,-0.020166352,-0.006313843,0.01965193,-0.036752004,0.018246563,0.0032040766,-0.032858774,-0.02936811,0.003507728,-0.0020098835,-0.037932746,-0.028082127,-0.0074353814,-0.044744074,-0.05029418,-0.013507515,-0.053258575,-0.06754793,-0.031762354,-0.006197407,-0.0049171173,0.004649667,-0.007451294,-0.045847077,0.004784882,-0.014025332,-0.06912964,-0.003976546,-0.018506242,-0.09086934,-0.013017468,0.019336285,-0.0032896362,0.0028174967,0.020759504,-0.032864377,-0.0073015615,0.018805044,-0.052799035,-0.023879275,0.016897758,-0.06973473,-0.03910017,0.03582557,0.014012847,-0.0018021166,0.0361417,-0.006732556,-0.010894313,0.02691563,-0.005607662,-0.029940337,0.022929767,0.010069666,-0.04251659))
        landmarksLibraryNames.add("ThreeR")
        landmarksLibrary.add(
            doubleArrayOf(0.012208762,0.09932865,0.005265169,-0.018922314,0.078014545,-0.0041272044,-0.029499143,0.04985249,-0.022132427,-0.028473144,0.014700949,-0.04254193,-0.007061327,-0.0050248643,-0.0532196,-0.030651769,0.0026388913,0.002408862,-0.038976733,-0.027941434,-0.004484758,-0.04328929,-0.047398124,-0.008867979,-0.05296649,-0.06418516,-0.029451251,-0.0051015113,-0.0036142436,0.0059722625,-0.0070332135,-0.046691447,1.8402934E-5,-0.014217673,-0.06978156,-0.012573935,-0.020179491,-0.090881884,-0.025692783,0.019823644,-0.004029029,-6.0308725E-4,0.019343702,-0.036833726,-0.01172477,0.013914687,-0.056779377,-0.028419852,0.011162079,-0.0745632,-0.039251015,0.034096807,0.01010518,-0.008142222,0.044356752,-0.013219932,-0.010894045,0.046576858,-0.032245293,-0.021452338,0.046290837,-0.047567282,-0.03593965))
        landmarksLibraryNames.add("Four")
        landmarksLibrary.add(
            doubleArrayOf(0.014618559,0.08960848,0.025381744,-0.017202925,0.07090211,0.009567834,-0.039315928,0.053928986,-6.412864E-4,-0.062238235,0.031813543,-0.013300259,-0.07977875,0.010934773,-0.01960653,-0.029963307,0.0030147973,0.0032387897,-0.039590854,-0.024468465,-0.007056311,-0.049776323,-0.041322056,-0.017114948,-0.05832504,-0.050300267,-0.04525186,-0.005221518,-0.0041810167,0.004972499,-0.009274974,-0.041888017,-0.0065225475,-0.016415147,-0.061385594,-0.029963784,-0.024458729,-0.078391664,-0.051560633,0.02029602,-0.0034182437,-0.0012810603,0.019608026,-0.03244055,-0.013150893,0.017046537,-0.05178754,-0.033470593,0.011254442,-0.067948096,-0.055441163,0.035071135,0.01199748,-0.0065262467,0.04459096,-0.0068809246,-0.010250583,0.049258146,-0.023423934,-0.02718845,0.04777255,-0.03383129,-0.046761334))
        landmarksLibraryNames.add("Five")
        landmarksLibrary.add(
            doubleArrayOf(0.018797567,0.08954433,0.03904234,-0.011703236,0.07365785,0.010739796,-0.016552227,0.051414207,-0.018211335,-0.016358277,0.030194728,-0.050406694,-0.013555489,0.007107136,-0.06500299,-0.030958412,0.0046092737,-0.0062236786,-0.0388808,-0.026156155,-0.007630214,-0.044356115,-0.047894716,-0.016937144,-0.05069803,-0.065200716,-0.046097472,-0.0061392183,-0.0032436792,0.0035395771,-0.0019084429,-0.02769315,-0.024575777,-0.0016264956,-0.014516735,-0.049343012,-0.008644666,0.00150138,-0.058182478,0.019730184,-0.0053458977,0.00505054,0.023501407,-0.02274018,-0.021141313,0.019466134,-0.012410257,-0.043972835,0.012397944,0.002030334,-0.05274768,0.03737563,0.010507257,0.0068057626,0.04680592,-0.012495636,7.567853E-4,0.05037527,-0.030286105,-0.014088005,0.051347118,-0.045572396,-0.02810891))
        landmarksLibraryNames.add("Fox1")
        landmarksLibrary.add(
            doubleArrayOf(0.02504077,0.08820416,-0.021786422,-0.0037034974,0.07262952,-0.005928129,-0.022673337,0.049761087,0.012659736,-0.04775862,0.030295614,0.036833715,-0.07200319,0.012028214,0.06870344,-0.015467016,0.0044217156,0.025057599,-0.021173378,-0.024101168,0.02305922,-0.025212802,-0.038399167,0.0190272,-0.03440897,-0.053202026,-0.0024382547,-5.2246836E-4,-0.0023191986,0.010053908,-0.025988134,-0.027354999,-0.007489335,-0.04891499,-0.0130647505,-0.02390834,-0.06684244,0.0052430276,-0.035339713,0.008022143,-0.005341042,-0.015735842,-0.012123475,-0.01850377,-0.032351494,-0.037993707,-0.010748899,-0.04567401,-0.059118822,0.0033285525,-0.051438786,0.013201721,0.00644942,-0.037579603,0.015746687,-0.014708422,-0.04064876,0.0073907385,-0.040377114,-0.041032,0.0033353046,-0.062913395,-0.04419961))
        landmarksLibraryNames.add("Fox2")


        textLayout.text = landmarksLibraryNames.size.toString()
    }

    override fun onResume() {
        super.onResume()
        if (inputSource == InputSource.CAMERA) {
            // Restarts the camera and the opengl surface rendering.
            cameraInput = CameraInput(this)
            cameraInput!!.setNewFrameListener { textureFrame: TextureFrame? ->
                hands!!.send(
                    textureFrame
                )
            }
            glSurfaceView!!.post { startCamera() }
            glSurfaceView!!.setVisibility(View.VISIBLE)
        } else if (inputSource == InputSource.VIDEO) {
            videoInput!!.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (inputSource == InputSource.CAMERA) {
            glSurfaceView!!.setVisibility(View.GONE)
            cameraInput!!.close()
        } else if (inputSource == InputSource.VIDEO) {
            videoInput!!.pause()
        }
    }

    private fun downscaleBitmap(originalBitmap: Bitmap): Bitmap {
        val aspectRatio = originalBitmap.width.toDouble() / originalBitmap.height
        var width = imageView!!.width
        var height = imageView!!.height
        if (imageView!!.width.toDouble() / imageView!!.height > aspectRatio) {
            width = (height * aspectRatio).toInt()
        } else {
            height = (width / aspectRatio).toInt()
        }
        return Bitmap.createScaledBitmap(originalBitmap, width, height, false)
    }

    @Throws(IOException::class)
    private fun rotateBitmap(inputBitmap: Bitmap?, imageData: InputStream?): Bitmap? {
        val orientation = ExifInterface(imageData!!)
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        if (orientation == ExifInterface.ORIENTATION_NORMAL) {
            return inputBitmap
        }
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> matrix.postRotate(0f)
        }
        return Bitmap.createBitmap(
            inputBitmap!!, 0, 0, inputBitmap.width, inputBitmap.height, matrix, true
        )
    }

    /** Sets up the UI components for the static image demo.  */
    private fun setupStaticImageDemoUiComponents() {
        // The Intent to access gallery and read images as bitmap.
        imageGetter = registerForActivityResult(
            StartActivityForResult()
        ) { result: ActivityResult ->
            val resultIntent = result.data
            if (resultIntent != null) {
                if (result.resultCode == RESULT_OK) {
                    var bitmap: Bitmap? = null
                    try {
                        bitmap = downscaleBitmap(
                            MediaStore.Images.Media.getBitmap(
                                this.contentResolver, resultIntent.data
                            )
                        )
                    } catch (e: IOException) {
                        Log.e(TAG, "Bitmap reading error:$e")
                    }
                    try {
                        val imageData = this.contentResolver.openInputStream(
                            resultIntent.data!!
                        )
                        bitmap = rotateBitmap(bitmap, imageData)
                    } catch (e: IOException) {
                        Log.e(TAG, "Bitmap rotation error:$e")
                    }
                    if (bitmap != null) {
                        hands!!.send(bitmap)
                    }
                }
            }
        }
        val loadImageButton = findViewById<Button>(R.id.button_load_picture)
        loadImageButton.setOnClickListener { v: View? ->
            if (inputSource != InputSource.IMAGE) {
                stopCurrentPipeline()
                setupStaticImageModePipeline()
            }
            // Reads images from gallery.
            val pickImageIntent = Intent(Intent.ACTION_PICK)
            pickImageIntent.setDataAndType(MediaStore.Images.Media.INTERNAL_CONTENT_URI, "image/*")
            imageGetter!!.launch(pickImageIntent)
        }
        imageView = HandsResultImageView(this)
    }

    /** Sets up core workflow for static image mode.  */
    private fun setupStaticImageModePipeline() {
        inputSource = InputSource.IMAGE
        // Initializes a new MediaPipe Hands solution instance in the static image mode.
        hands = Hands(
            this,
            HandsOptions.builder()
                .setStaticImageMode(true)
                .setMaxNumHands(2)
                .setRunOnGpu(RUN_ON_GPU)
                .build()
        )

        // Connects MediaPipe Hands solution to the user-defined HandsResultImageView.
        hands!!.setResultListener { handsResult: HandsResult ->
            logWristLandmark(handsResult,  /*showPixelValues=*/true)
            imageView!!.setHandsResult(handsResult)
            runOnUiThread { imageView!!.update() }
        }
        hands!!.setErrorListener { message: String, e: RuntimeException? ->
            Log.e(
                TAG,
                "MediaPipe Hands error:$message"
            )
        }

        // Updates the preview layout.
        val frameLayout = findViewById<FrameLayout>(R.id.preview_display_layout)
        frameLayout.removeAllViewsInLayout()
        imageView!!.setImageDrawable(null)
        frameLayout.addView(imageView)
        imageView!!.visibility = View.VISIBLE
    }

    /** Sets up the UI components for the video demo.  */
    private fun setupVideoDemoUiComponents() {
        // The Intent to access gallery and read a video file.
        videoGetter = registerForActivityResult(
            StartActivityForResult()
        ) { result: ActivityResult ->
            val resultIntent = result.data
            if (resultIntent != null) {
                if (result.resultCode == RESULT_OK) {
                    glSurfaceView!!.post {
                        videoInput!!.start(
                            this,
                            resultIntent.data,
                            hands!!.glContext,
                            glSurfaceView!!.width,
                            glSurfaceView!!.height
                        )
                    }
                }
            }
        }
        val loadVideoButton = findViewById<Button>(R.id.button_load_video)
        loadVideoButton.setOnClickListener { v: View? ->
            stopCurrentPipeline()
            setupStreamingModePipeline(InputSource.VIDEO)
            // Reads video from gallery.
            val pickVideoIntent = Intent(Intent.ACTION_PICK)
            pickVideoIntent.setDataAndType(MediaStore.Video.Media.INTERNAL_CONTENT_URI, "video/*")
            videoGetter!!.launch(pickVideoIntent)
        }
    }

    /** Sets up the UI components for the live demo with camera input.  */
    private fun setupLiveDemoUiComponents() {
        val startCameraButton = findViewById<Button>(R.id.button_start_camera)
        startCameraButton.setOnClickListener { v: View? ->
            if (inputSource == InputSource.CAMERA) {
                return@setOnClickListener
            }
            stopCurrentPipeline()
            setupStreamingModePipeline(InputSource.CAMERA)
        }
    }

    /** Sets up core workflow for streaming mode.  */
    private fun setupStreamingModePipeline(inputSource: InputSource) {
        this.inputSource = inputSource
        // Initializes a new MediaPipe Hands solution instance in the streaming mode.
        hands = Hands(
            this,
            HandsOptions.builder()
                .setStaticImageMode(false)
                .setMaxNumHands(2)
                .setRunOnGpu(RUN_ON_GPU)
                .build()
        )
        hands!!.setErrorListener { message: String, e: RuntimeException? ->
            Log.e(
                TAG,
                "MediaPipe Hands error:$message"
            )
        }
        if (inputSource == InputSource.CAMERA) {
            cameraInput = CameraInput(this)
            cameraInput!!.setNewFrameListener { textureFrame: TextureFrame? ->
                hands!!.send(
                    textureFrame
                )
            }
        } else if (inputSource == InputSource.VIDEO) {
            videoInput = VideoInput(this)
            videoInput!!.setNewFrameListener { textureFrame: TextureFrame? ->
                hands!!.send(
                    textureFrame
                )
            }
        }

        // Initializes a new Gl surface view with a user-defined HandsResultGlRenderer.
        glSurfaceView = SolutionGlSurfaceView(this, hands!!.glContext, hands!!.glMajorVersion)
        glSurfaceView!!.setSolutionResultRenderer(HandsResultGlRenderer())
        glSurfaceView!!.setRenderInputImage(true)
        val textLayout = findViewById<TextView>(R.id.pars)
        hands!!.setResultListener { handsResult: HandsResult ->
            parseLandmarkToGesture(handsResult, textLayout)
            logWristLandmark(handsResult,  /*showPixelValues=*/false)
            glSurfaceView!!.setRenderData(handsResult)
            glSurfaceView!!.requestRender()
        }

        // The runnable to start camera after the gl surface view is attached.
        // For video input source, videoInput.start() will be called when the video uri is available.
        if (inputSource == InputSource.CAMERA) {
            glSurfaceView!!.post { startCamera() }
        }

        // Updates the preview layout.
        val frameLayout = findViewById<FrameLayout>(R.id.preview_display_layout)
        imageView!!.visibility = View.GONE
        frameLayout.removeAllViewsInLayout()
        frameLayout.addView(glSurfaceView)
        glSurfaceView!!.visibility = View.VISIBLE
        frameLayout.requestLayout()
    }

    private fun startCamera() {
        cameraInput!!.start(
            this,
            hands!!.glContext,
            CameraInput.CameraFacing.FRONT,
            glSurfaceView!!.width,
            glSurfaceView!!.height
        )
    }

    private fun stopCurrentPipeline() {
        if (cameraInput != null) {
            cameraInput!!.setNewFrameListener(null)
            cameraInput!!.close()
        }
        if (videoInput != null) {
            videoInput!!.setNewFrameListener(null)
            videoInput!!.close()
        }
        if (glSurfaceView != null) {
            glSurfaceView!!.visibility = View.GONE
        }
        if (hands != null) {
            hands!!.close()
        }
    }

    private fun parseLandmarkToGesture(result: HandsResult, textLayout: TextView) {
        if (result.multiHandLandmarks().isEmpty()) {
            return
        }
        var min_distance = 100000000000.0
        var min_distance_i = 0
        val lm  = result.multiHandWorldLandmarks()[0].landmarkList;
        for (j in landmarksLibrary.indices) {
            val a = landmarksLibrary[j]
            var distance = 0.0

            for (i in lm.indices) {
                distance += (lm[i].x - a[i*3])*(lm[i].x - a[i*3])+
                        (lm[i].y - a[i*3+1])*(lm[i].y - a[i*3+1])
//                        +
//                        (lm[i].z - a[i*3+2])*(lm[i].z - a[i*3+2])
            }
            if (distance < min_distance) {
                min_distance = distance
                min_distance_i =j
            }
        }
        textLayout.text = "%s %.${2}f".format(landmarksLibraryNames[min_distance_i], min_distance*1000)
    }

    private fun logWristLandmark(result: HandsResult, showPixelValues: Boolean) {
        if (result.multiHandLandmarks().isEmpty()) {
            return
        }
        val wristLandmark = result.multiHandLandmarks()[0].landmarkList[HandLandmark.WRIST]
        // For Bitmaps, show the pixel values. For texture inputs, show the normalized coordinates.
        if (showPixelValues) {
            val width = result.inputBitmap().width
            val height = result.inputBitmap().height
            Log.i(
                TAG, String.format(
                    "MediaPipe Hand wrist coordinates (pixel values): x=%f, y=%f",
                    wristLandmark.x * width, wristLandmark.y * height
                )
            )
        } else {
            Log.i(
                TAG, String.format(
                    "MediaPipe Hand wrist normalized coordinates (value range: [0, 1]): x=%f, y=%f",
                    wristLandmark.x, wristLandmark.y
                )
            )
        }
        if (result.multiHandWorldLandmarks().isEmpty()) {
            return
        }
        val wristWorldLandmark =
            result.multiHandWorldLandmarks()[0].landmarkList[HandLandmark.WRIST]
        val sb = StringBuilder()
        sb.append("{")
        for (lm in result.multiHandWorldLandmarks()[0].landmarkList) {
            sb.append(lm.x)
            sb.append(",")
            sb.append(lm.y)
            sb.append(",")
            sb.append(lm.z)
            sb.append(",")
        }
        sb.append("}\n")
        Log.i(
            TAG, String.format(
                "MediaPipe Hand wrist world coordinates (in meters with the origin at the hand's"
                        + " approximate geometric center): x=%f m, y=%f m, z=%f m %s",
                wristWorldLandmark.x, wristWorldLandmark.y, wristWorldLandmark.z, sb
            )
        )
    }

    companion object {
        private const val TAG = "MainActivity"

        // Run the pipeline and the model inference on GPU or CPU.
        private const val RUN_ON_GPU = true
    }
}