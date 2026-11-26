import sistemainventario.service.ListaArchivoReader;
import java.nio.file.*;
public class PruebaListaArchivoReader { public static void main(String[] a) throws Exception {
  String t = ListaArchivoReader.aTexto("x.xlsx", Files.readAllBytes(Path.of(a[0])));
  String[] l = t.split("\n"); System.out.println(l.length+" filas"); for (int i=0;i<Math.min(4,l.length);i++) System.out.println(l[i].replace("\t"," | "));
  System.out.println(l[l.length-1].replace("\t"," | "));
  System.out.println(ListaArchivoReader.aTexto("a.csv","Nombre;Costo\nCamisa;52.000\n".getBytes("ISO-8859-1")).replace("\n","/"));
  try { ListaArchivoReader.aTexto("a.xlsx","PKxx".getBytes()); } catch (Exception e){ System.out.println(e.getMessage()); }
}}
