package com.splitttr.docs.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.splitttr.docs.dto.*;
import com.splitttr.docs.service.AuthService;
import com.splitttr.docs.service.DocumentService;

@Path("/api/documents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DocumentResource {

    @Inject
    DocumentService service;

    @Inject
    AuthService auth;

    @GET
    public Response list() {
        // List only the current user's documents
        String ownerId = auth.getCurrentUserId();
        var docs = service.listByOwner(ownerId);
        return Response.ok(docs.stream().map(DocumentResponse::from).toList()).build();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") String id) {
        String ownerId = auth.getCurrentUserId();
        return service.getById(id)
            .filter(doc -> doc.ownerId.equals(ownerId)) // Only allow access to own docs
            .map(doc -> Response.ok(DocumentResponse.from(doc)).build())
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    public Response create(DocumentCreateRequest req) {
        // ownerId comes from JWT, not from request body
        String ownerId = auth.getCurrentUserId();
        var doc = service.create(req.title(), req.content(), ownerId);
        return Response.status(Response.Status.CREATED)
            .entity(DocumentResponse.from(doc))
            .build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") String id, DocumentUpdateRequest req) {
        String ownerId = auth.getCurrentUserId();
        
        // Verify ownership before update
        var existing = service.getById(id);
        if (existing.isEmpty() || !existing.get().ownerId.equals(ownerId)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        
        return service.update(id, req.title(), req.content())
            .map(doc -> Response.ok(DocumentResponse.from(doc)).build())
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        String ownerId = auth.getCurrentUserId();
        
        // Verify ownership before delete
        var existing = service.getById(id);
        if (existing.isEmpty() || !existing.get().ownerId.equals(ownerId)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        
        return service.delete(id)
            ? Response.noContent().build()
            : Response.status(Response.Status.NOT_FOUND).build();
    }
}
