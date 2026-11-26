package sistemainventario.service;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Convierte un archivo de Excel (.xlsx) o de texto (.csv, .txt) en el texto
 * separado por tabuladores que ya entiende "Varios desde Excel".
 *
 * <p>No guarda nada y no usa librerías externas: un .xlsx es un zip con XML.
 * Así el archivo pasa por la misma revisión y las mismas reglas que una lista
 * pegada (<b>GRASP Protected Variations</b>: una sola puerta para cargar
 * productos). Los números salen en formato colombiano (coma decimal) para que
 * el importador no confunda 89.5 con 895.</p>
 */
public final class ListaArchivoReader {

    /** Tope de lo que se acepta del archivo (el importador ya limita a 500 filas). */
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final int MAX_FILAS = 2000;
    private static final long MAX_XML = 40L * 1024 * 1024;

    private ListaArchivoReader() { }

    /** Lanzada con un mensaje listo para mostrar a la persona. */
    public static final class ArchivoInvalido extends Exception {
        public ArchivoInvalido(String mensaje) { super(mensaje); }
    }

    /** @return el contenido del archivo como texto con tabuladores, una fila por línea */
    public static String aTexto(String nombre, byte[] datos) throws ArchivoInvalido {
        if (datos == null || datos.length == 0) { throw new ArchivoInvalido("El archivo está vacío."); }
        if (datos.length > MAX_BYTES) { throw new ArchivoInvalido("El archivo es muy grande (máximo 5 MB)."); }
        boolean zip = datos.length > 3 && datos[0] == 'P' && datos[1] == 'K';
        String n = nombre == null ? "" : nombre.toLowerCase(Locale.ROOT);
        if (zip) { return deXlsx(datos); }
        if (n.endsWith(".xls")) { throw new ArchivoInvalido("Ese es un Excel antiguo (.xls). Guárdelo como .xlsx y vuelva a subirlo."); }
        return deTexto(datos);
    }

    // ── texto plano ─────────────────────────────────────────────────────

    private static String deTexto(byte[] datos) throws ArchivoInvalido {
        int ini = (datos.length >= 3 && (datos[0] & 0xFF) == 0xEF && (datos[1] & 0xFF) == 0xBB && (datos[2] & 0xFF) == 0xBF) ? 3 : 0;
        String t;
        try {
            t = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(datos, ini, datos.length - ini)).toString();
        } catch (CharacterCodingException e) {
            t = new String(datos, ini, datos.length - ini, StandardCharsets.ISO_8859_1);
        }
        for (int i = 0; i < Math.min(t.length(), 200); i++) {
            if (t.charAt(i) == 0) { throw new ArchivoInvalido("No es un archivo de Excel ni de texto. Use .xlsx o .csv."); }
        }
        return t.replace("\r\n", "\n").replace('\r', '\n');
    }

    // ── .xlsx ───────────────────────────────────────────────────────────

    private static String deXlsx(byte[] datos) throws ArchivoInvalido {
        Map<String, byte[]> partes = new HashMap<>();
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(datos))) {
            ZipEntry e;
            long total = 0;
            while ((e = zin.getNextEntry()) != null) {
                String nom = e.getName();
                if (!(nom.equals("xl/workbook.xml") || nom.equals("xl/_rels/workbook.xml.rels")
                        || nom.equals("xl/sharedStrings.xml") || nom.startsWith("xl/worksheets/"))) { continue; }
                byte[] b = leer(zin, MAX_XML - total);
                total += b.length;
                partes.put(nom, b);
            }
        } catch (IOException e) {
            throw new ArchivoInvalido("No se pudo abrir el archivo. ¿Está dañado o protegido con contraseña?");
        }
        if (!partes.containsKey("xl/workbook.xml")) { throw new ArchivoInvalido("Ese archivo no parece un Excel (.xlsx)."); }
        try {
            List<String> compartidas = cadenasCompartidas(partes.get("xl/sharedStrings.xml"));
            List<String[]> hojas = listarHojas(partes);   // {nombre, ruta}
            if (hojas.isEmpty()) { throw new ArchivoInvalido("El Excel no tiene hojas."); }
            String[] elegida = elegirHoja(hojas);
            byte[] xml = partes.get(elegida[1]);
            if (xml == null) { throw new ArchivoInvalido("No se pudo leer la hoja \"" + elegida[0] + "\"."); }
            String texto = filas(xml, compartidas);
            if (texto.isBlank()) { throw new ArchivoInvalido("La hoja \"" + elegida[0] + "\" no tiene datos."); }
            return texto;
        } catch (ArchivoInvalido e) {
            throw e;
        } catch (Exception e) {
            throw new ArchivoInvalido("No se pudo leer el Excel. Pruebe guardarlo de nuevo como .xlsx.");
        }
    }

    private static byte[] leer(InputStream in, long limite) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
            if (out.size() > limite) { throw new IOException("demasiado grande"); }
        }
        return out.toByteArray();
    }

    private static Document xml(byte[] b) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setNamespaceAware(false);
        return f.newDocumentBuilder().parse(new ByteArrayInputStream(b));
    }

    private static List<String> cadenasCompartidas(byte[] b) throws Exception {
        List<String> r = new ArrayList<>();
        if (b == null) { return r; }
        NodeList si = xml(b).getElementsByTagName("si");
        for (int i = 0; i < si.getLength(); i++) { r.add(textoDe((Element) si.item(i))); }
        return r;
    }

    /** Une los {@code <t>} de una celda de texto (ignora la vista fonética {@code <rPh>}). */
    private static String textoDe(Element el) {
        StringBuilder sb = new StringBuilder();
        for (Node c = el.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (c.getNodeType() != Node.ELEMENT_NODE) { continue; }
            String tag = c.getNodeName();
            if (tag.equals("t")) { sb.append(c.getTextContent()); }
            else if (tag.equals("r")) { sb.append(textoDe((Element) c)); }
        }
        return sb.toString();
    }

    private static List<String[]> listarHojas(Map<String, byte[]> partes) throws Exception {
        Map<String, String> rutas = new HashMap<>();
        byte[] rels = partes.get("xl/_rels/workbook.xml.rels");
        if (rels != null) {
            NodeList rl = xml(rels).getElementsByTagName("Relationship");
            for (int i = 0; i < rl.getLength(); i++) {
                Element e = (Element) rl.item(i);
                String t = e.getAttribute("Target");
                t = t.startsWith("/") ? t.substring(1) : "xl/" + t;
                rutas.put(e.getAttribute("Id"), t);
            }
        }
        List<String[]> r = new ArrayList<>();
        NodeList sh = xml(partes.get("xl/workbook.xml")).getElementsByTagName("sheet");
        for (int i = 0; i < sh.getLength(); i++) {
            Element e = (Element) sh.item(i);
            if ("hidden".equals(e.getAttribute("state")) || "veryHidden".equals(e.getAttribute("state"))) { continue; }
            String ruta = rutas.get(e.getAttribute("r:id"));
            if (ruta == null) { ruta = "xl/worksheets/sheet" + (i + 1) + ".xml"; }
            r.add(new String[] {e.getAttribute("name"), ruta});
        }
        return r;
    }

    /** La hoja "Importar…"/"Productos" si existe; si no, la primera. */
    private static String[] elegirHoja(List<String[]> hojas) {
        for (String[] h : hojas) {
            String n = h[0].toLowerCase(Locale.ROOT);
            if (n.contains("importar") || n.equals("productos")) { return h; }
        }
        return hojas.get(0);
    }

    private static String filas(byte[] hoja, List<String> compartidas) throws Exception {
        NodeList rows = xml(hoja).getElementsByTagName("row");
        StringBuilder out = new StringBuilder();
        int escritas = 0;
        for (int i = 0; i < rows.getLength() && escritas < MAX_FILAS; i++) {
            Element row = (Element) rows.item(i);
            List<String> celdas = new ArrayList<>();
            for (Node c = row.getFirstChild(); c != null; c = c.getNextSibling()) {
                if (c.getNodeType() != Node.ELEMENT_NODE || !c.getNodeName().equals("c")) { continue; }
                Element ce = (Element) c;
                int col = columna(ce.getAttribute("r"));
                if (col < 0) { col = celdas.size(); }
                while (celdas.size() < col) { celdas.add(""); }
                celdas.add(valor(ce, compartidas));
            }
            while (!celdas.isEmpty() && celdas.get(celdas.size() - 1).isEmpty()) { celdas.remove(celdas.size() - 1); }
            if (celdas.isEmpty()) { continue; }
            out.append(String.join("\t", celdas)).append('\n');
            escritas++;
        }
        return out.toString();
    }

    private static int columna(String ref) {
        int n = 0, i = 0;
        while (i < ref.length() && Character.isLetter(ref.charAt(i))) {
            n = n * 26 + (Character.toUpperCase(ref.charAt(i)) - 'A' + 1);
            i++;
        }
        return i == 0 ? -1 : n - 1;
    }

    private static String valor(Element c, List<String> compartidas) {
        String t = c.getAttribute("t");
        if (t.equals("inlineStr")) {
            NodeList is = c.getElementsByTagName("is");
            return is.getLength() > 0 ? limpiar(textoDe((Element) is.item(0))) : "";
        }
        NodeList vs = c.getElementsByTagName("v");
        if (vs.getLength() == 0) { return ""; }
        String v = vs.item(0).getTextContent().trim();
        switch (t) {
            case "s":
                try { return limpiar(compartidas.get(Integer.parseInt(v))); } catch (RuntimeException e) { return ""; }
            case "b": return v.equals("1") ? "Sí" : "No";
            case "e": return "";
            case "str": return limpiar(v);
            default: return numero(v);
        }
    }

    /** 52000 → "52000"; 89.5 → "89,5" (la coma decimal es la que lee TBalance). */
    private static String numero(String v) {
        try {
            BigDecimal d = new BigDecimal(v).stripTrailingZeros();
            if (d.scale() < 0) { d = d.setScale(0); }
            if (d.scale() > 4) { d = d.setScale(4, java.math.RoundingMode.HALF_UP).stripTrailingZeros(); if (d.scale() < 0) { d = d.setScale(0); } }
            return d.toPlainString().replace('.', ',');
        } catch (NumberFormatException e) {
            return limpiar(v);
        }
    }

    private static String limpiar(String s) {
        return s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim();
    }
}
