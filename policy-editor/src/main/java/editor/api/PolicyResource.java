package editor.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import editor.model.Policy;
import editor.service.AuditService;
import editor.service.HistoryService;
import editor.service.PolicyService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * REST API 端点，提供策略的 CRUD 操作
 */
@Path("/api/policies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PolicyResource {

    @Inject
    PolicyService policyService;

    @Inject
    HistoryService historyService;

    @Inject
    AuditService auditService;

    @Inject
    ObjectMapper objectMapper;

    /**
     * 获取所有策略
     */
    @GET
    @PermitAll
    public List<Policy> getAllPolicies() {
        return policyService.getAllPolicies();
    }

    /**
     * 根据 ID 获取策略
     */
    @GET
    @Path("/{id}")
    @PermitAll
    public Response getPolicyById(@PathParam("id") String id) {
        return policyService.getPolicyById(id)
            .map(policy -> Response.ok(policy).build())
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * 创建新策略
     */
    @POST
    @RolesAllowed("admin")
    public Response createPolicy(Policy policy) {
        try {
            Policy created = policyService.createPolicy(policy);
            return Response.status(Response.Status.CREATED).entity(created).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("创建策略失败: " + e.getMessage())
                .build();
        }
    }

    /**
     * 更新现有策略
     */
    @PUT
    @Path("/{id}")
    @RolesAllowed("admin")
    public Response updatePolicy(@PathParam("id") String id, Policy policy) {
        return policyService.updatePolicy(id, policy)
            .map(updated -> Response.ok(updated).build())
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * 删除策略
     */
    @DELETE
    @Path("/{id}")
    @RolesAllowed("admin")
    public Response deletePolicy(@PathParam("id") String id) {
        boolean deleted = policyService.deletePolicy(id);
        return deleted
            ? Response.noContent().build()
            : Response.status(Response.Status.NOT_FOUND).build();
    }

    // 历史：列出版本名
    @GET
    @Path("/{id}/history")
    @PermitAll
    public List<String> history(@PathParam("id") String id) {
        return historyService.listVersionNames(id);
    }

    // 历史：加载某版本内容
    @GET
    @Path("/{id}/history/{ver}")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response historyVersion(@PathParam("id") String id, @PathParam("ver") String ver) {
        try { return Response.ok(historyService.loadVersion(id, ver)).build(); }
        catch (Exception e) { return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build(); }
    }

    // 撤销/重做
    @POST
    @Path("/{id}/undo")
    @RolesAllowed("admin")
    public Response undo(@PathParam("id") String id) {
        Path policyPath = historyTempFile(id);
        boolean ok = historyService.undo(id, policyPath);
        if (!ok) {
            return Response.status(Response.Status.CONFLICT).entity("无法撤销").build();
        }
        try {
            Policy restored = objectMapper.readValue(policyPath.toFile(), Policy.class);
            Files.deleteIfExists(policyPath);
            policyService.updatePolicy(id, restored).orElseGet(() -> policyService.createPolicy(restored));
            return Response.ok().build();
        } catch (Exception e) {
            return Response.serverError().entity("应用历史版本失败: " + e.getMessage()).build();
        }
    }

    @POST
    @Path("/{id}/redo")
    @RolesAllowed("admin")
    public Response redo(@PathParam("id") String id) {
        Path policyPath = historyTempFile(id);
        boolean ok = historyService.redo(id, policyPath);
        if (!ok) {
            return Response.status(Response.Status.CONFLICT).entity("无法重做").build();
        }
        try {
            Policy restored = objectMapper.readValue(policyPath.toFile(), Policy.class);
            Files.deleteIfExists(policyPath);
            policyService.updatePolicy(id, restored).orElseGet(() -> policyService.createPolicy(restored));
            return Response.ok().build();
        } catch (Exception e) {
            return Response.serverError().entity("应用历史版本失败: " + e.getMessage()).build();
        }
    }

    // 审计日志查看/清空
    @GET
    @Path("/audit")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("admin")
    public java.util.List<editor.service.AuditService.AuditEntry> audit(@QueryParam("page") @DefaultValue("0") int page,
                                                                        @QueryParam("size") @DefaultValue("100") int size,
                                                                        @QueryParam("q") String q) {
        return auditService.query(page, size, q);
    }

    @POST
    @Path("/audit/clear")
    @RolesAllowed("admin")
    public Response clearAudit() {
        auditService.clear();
        return Response.ok().build();
    }

    @GET
    @Path("/audit/export")
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed("admin")
    public Response exportAudit(@QueryParam("start") String start,
                                @QueryParam("end") String end,
                                @QueryParam("q") String q) {
        String body = auditService.exportText(start, end, q);
        return Response.ok(body)
                .header("Content-Disposition", "attachment; filename=audit.log")
                .build();
    }

    /** 导出所有策略为 ZIP */
    @GET
    @Path("/export")
    @Produces("application/zip")
    @PermitAll
    public Response exportZip() {
        try {
            java.nio.file.Path tmp = Files.createTempFile("policies-", ".zip");
            policyService.exportZip(tmp);
            byte[] data = Files.readAllBytes(tmp);
            Files.deleteIfExists(tmp);
            return Response.ok(new ByteArrayInputStream(data))
                    .header("Content-Disposition", "attachment; filename=policies.zip")
                    .build();
        } catch (Exception e) {
            return Response.serverError().entity("导出失败: " + e.getMessage()).build();
        }
    }

    /** 导入 ZIP（base64） */
    public static class ImportZipBody { public String base64; }

    @POST
    @Path("/importZip")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("admin")
    public Response importZip(ImportZipBody body) {
        try {
            byte[] bytes = java.util.Base64.getDecoder().decode(body.base64);
            policyService.importZip(new ByteArrayInputStream(bytes));
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("导入失败: " + e.getMessage()).build();
        }
    }

    /** 同步：从远端目录拉取/推送（设置页配置的目录） */
    @POST
    @Path("/sync/pull")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("admin")
    public Response syncPull(String remoteDir) {
        try { var r = policyService.syncPullWithResult(remoteDir); return Response.ok(r).build(); }
        catch (Exception e) { return Response.serverError().entity("{\"error\":\""+e.getMessage()+"\"}").build(); }
    }

    @POST
    @Path("/sync/push")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("admin")
    public Response syncPush(String remoteDir) {
        try { var r = policyService.syncPushWithResult(remoteDir); return Response.ok(r).build(); }
        catch (Exception e) { return Response.serverError().entity("{\"error\":\""+e.getMessage()+"\"}").build(); }
    }

    private Path historyTempFile(String id) {
        return Paths.get("data", "history", "tmp", id + ".json");
    }
}
