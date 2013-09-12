package control.admin;

import modelo.Producto;
import control.Tools;
import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;
import persistencia.PersistenceInterface;

/**
 * @author Juan Díez-Yanguas Barber
 */
public class EditProductServlet extends HttpServlet {
    
    private boolean validateForm (HttpServletRequest request) throws IOException, ServletException{
        if(request.getParts().size() >= 7 && request.getPart("codigo") != null && request.getPart("name") != null
                && request.getPart("price") != null && request.getPart("stock") != null && 
                request.getPart("desc") != null && request.getPart("detail") != null &&
                request.getPart("sendProd") != null){
            return Tools.validateUUID(Tools.getcontentPartText(request.getPart("codigo")));
        }
        return false;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (request.getSession().getAttribute("productoEnCursoEdit") == null) {
            //Si he intentado entrar en la pagina directamente, la pagina no existe
            response.sendError(404);
        } else {
            Producto prod = (Producto) request.getSession().getAttribute("productoEnCursoEdit");
            PersistenceInterface persistencia = (PersistenceInterface) request.getServletContext().getAttribute("persistence");
            boolean ok = persistencia.updateProduct(prod.getCodigo(), prod);
            request.getSession().removeAttribute("productoEnCursoEdit");

            request.setAttribute("resultados", "Resultados de la operación");
            if (ok == true) {
                Tools.anadirMensaje(request, "El producto se ha editado correctamente");
            } else {
                Tools.anadirMensaje(request, "Ha ocurrido un error al modificar el producto. El producto no ha sido encontrado");
            }
            RequestDispatcher correcto = request.getRequestDispatcher("/admin/administration/products_administration.jsp");
            correcto.forward(request, response);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (validateForm(request) == true) {
            try {
                String codigo = Tools.getcontentPartText(request.getPart("codigo"));
                String nombre = Tools.validateProdText(Tools.getcontentPartText(request.getPart("name")), 70, "Nombre");
                double precio = Tools.validatePrice(Tools.getcontentPartText(request.getPart("price")));
                int nStock = Tools.validateNumber(Tools.getcontentPartText(request.getPart("stock")), "Unidades disponibles");
                String descripcion = Tools.validateProdText(Tools.getcontentPartText(request.getPart("desc")), 100, "Descripción");
                String detalles = Tools.getContentTextArea(request.getPart("detail"));
                Tools.validateHTML(detalles);                
                if (request.getPart("foto").getSize() > 0) {
                    Tools.borrarImagenDeProdructoDelSistemaDeFicheros(request.getServletContext().getRealPath("/images/products/" + codigo));
                    if (request.getPart("foto").getContentType().contains("image") == false || request.getPart("foto").getSize() > 8388608) {
                        request.setAttribute("resultados", "Archivo no válido");
                        Tools.anadirMensaje(request, "Solo se admiten archivos de tipo imagen");
                        Tools.anadirMensaje(request, "El tamaño máximo de archivo son 8 Mb");
                        request.getRequestDispatcher("/admin/administration/products_administration.jsp").forward(request, response);
                        return;
                    }else{
                        String fileName = this.getServletContext().getRealPath("/images/products/" + codigo);
                        boolean ok = Tools.guardarImagenDeProdructoEnElSistemaDeFicheros(request.getPart("foto").getInputStream(), fileName);
                        if (ok == false){
                            request.setAttribute("resultados", "Fallo al guardar archivo");
                            Tools.anadirMensaje(request, "Ocurrio un error guardando la imagen");
                            request.getRequestDispatcher("/admin/administration/products_administration.jsp").forward(request, response);
                            return;
                        }
                    }
                }else{
                    if (request.getPart("conserv") != null && Tools.getcontentPartText(request.getPart("conserv")).equals("conservarImagen") == true){
                        
                    }else{
                        Tools.borrarImagenDeProdructoDelSistemaDeFicheros(request.getServletContext().getRealPath("/images/products/" + codigo));
                    }
                }
                Producto prod = new Producto(codigo, nombre, precio, nStock, descripcion, detalles);
                request.getSession().setAttribute("productoEnCursoEdit", prod);
                request.setAttribute("operation", "edit");
                request.getRequestDispatcher("/WEB-INF/admin/preview_prod.jsp").forward(request, response);                
            } catch (IntrusionException ex) {
                request.setAttribute("resultados", "Intrusión detectada");
                Tools.anadirMensaje(request, ex.getUserMessage());
                request.getRequestDispatcher("/admin/administration/products_administration.jsp").forward(request, response);
            } catch (ValidationException ex) {
                request.setAttribute("resultados", "Validación de formulario fallida");
                Tools.anadirMensaje(request, ex.getUserMessage());
                request.getRequestDispatcher("/admin/administration/products_administration.jsp").forward(request, response);
            }
        }
    }
    
    @Override
    public String getServletInfo (){
        return "Servlet encargado de la edición de productos";
    }
}
