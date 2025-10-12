package editor.api;

import editor.model.Policy;
import editor.service.PolicyService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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

    /**
     * 获取所有策略
     */
    @GET
    public List<Policy> getAllPolicies() {
        return policyService.getAllPolicies();
    }

    /**
     * 根据 ID 获取策略
     */
    @GET
    @Path("/{id}")
    public Response getPolicyById(@PathParam("id") String id) {
        return policyService.getPolicyById(id)
            .map(policy -> Response.ok(policy).build())
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * 创建新策略
     */
    @POST
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
    public Response deletePolicy(@PathParam("id") String id) {
        boolean deleted = policyService.deletePolicy(id);
        return deleted
            ? Response.noContent().build()
            : Response.status(Response.Status.NOT_FOUND).build();
    }
}
