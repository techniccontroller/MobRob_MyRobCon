package cv_bridge;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("Convert2Diamond")
public enum Format { JPG("jpg"), JPEG("jpeg"), JPE("jpe"), PNG("png"), BMP("bmp"), DIP("dip"), PPM("ppm"), PGM("pgm"), PBM("pbm"),
    JP2("jp2"), SR("sr"), RAS("ras"), TIF("tif"), TIFF("TIFF") ; // this formats rviz is not support.
    protected String strFormat;

    static private Map<Format, String> map = new HashMap<Format, String>();
    static {
        for (Format format : Format.values()) {
            map.put(format, format.strFormat);
        }
    }

    Format(final String strFormat) { this.strFormat = strFormat; }

    public static String valueOf(Format format) {
        return map.get(format);
    }

    static String getExtension(Format format){
        String ext = ".";
        return ext.concat(map.get(format));
    }
}