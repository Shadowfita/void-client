/* Class216 - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */

final class Class216 implements Interface13 {
    int anInt4974;
    static Js5Archive aClass45_4975;
    static int anInt4976;
    static int anInt4977;
    int anInt4978;
    static int anInt4979;
    float[] aFloatArray4980;

    static final void method1583(byte i) {
        anInt4977++;
        if (r.anInt9721 != -1) {
            boolean interfaceScaled = Applet_Sub1.beginInterfaceInputScale();
            try {
                int i_0_ = Class258_Sub4.aClass373_8552.method3597(true);
                int i_1_ = Class258_Sub4.aClass373_8552.method3594((byte) 72);
                Class348_Sub45 class348_sub45 = ((Class348_Sub45) Class318_Sub1_Sub3.aClass262_8744.method1995(4));
                if (class348_sub45 != null) {
                    i_0_ = class348_sub45.method3308((byte) -128);
                    i_1_ = class348_sub45.method3311(58);
                }
                int i_2_ = 0;
                if (i != -73) anInt4976 = 105;
                int i_3_ = 0;
                if (Class59_Sub1.aBoolean5300) {
                    i_2_ = s_Sub3.method4008((byte) -128);
                    i_3_ = Class16.method260(false);
                }
                int interfaceWidth = interfaceScaled ? Applet_Sub1.getInterfaceLogicalWidth() : Class321.anInt4017;
                int interfaceHeight = interfaceScaled ? Applet_Sub1.getInterfaceLogicalHeight() : Class348_Sub42_Sub8_Sub2.anInt10432;
                int interfaceX = interfaceScaled ? Applet_Sub1.physicalToInterfaceX(i_2_) : i_2_;
                int interfaceY = interfaceScaled ? Applet_Sub1.physicalToInterfaceY(i_3_) : i_3_;
                Class182.method1373(r.anInt9721, interfaceX, interfaceX, interfaceWidth + interfaceX, i_1_, i_0_, -1391, i_0_ + interfaceX, interfaceHeight + interfaceY, interfaceY, interfaceY + i_1_, interfaceY);
                if (Class168.aClass46_2249 != null) Class228.method1630(0, interfaceY + i_1_, interfaceX + i_0_);
            } finally {
                if (interfaceScaled) {
                    Applet_Sub1.endInterfaceInputScale();
                }
            }
        }
    }

    public static void method1584(byte i) {
        if (i != -64) method1584((byte) -48);
        aClass45_4975 = null;
    }

    Class216(int i, int i_4_) {
        this.anInt4978 = i_4_;
        this.anInt4974 = i;
        this.aFloatArray4980 = new float[i * i_4_];
    }
}
