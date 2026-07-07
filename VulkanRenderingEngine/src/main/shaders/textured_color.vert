#version 450

layout(location = 0) in vec2 inPos;
layout(location = 1) in vec2 inUV;
layout(location = 2) in vec4 inColor;
layout(location = 3) in vec4 inInstanceModelRow0; // m00, m01, translate x, z
layout(location = 4) in vec4 inInstanceModelRow1; // m10, m11, translate y, unused
layout(location = 5) in vec4 inInstanceUVRect;    // offset u, offset v, scale u, scale v
layout(location = 6) in vec4 inInstanceColor;

layout(location = 0) out vec2 vUV;
layout(location = 1) out vec4 vColor;

layout(set = 0, binding = 0) uniform Camera {
    mat4 proj;
};

void main() {
    vec2 worldXY = vec2(
        dot(inInstanceModelRow0.xy, inPos),
        dot(inInstanceModelRow1.xy, inPos)
    ) + vec2(inInstanceModelRow0.z, inInstanceModelRow1.z);

    vUV = inInstanceUVRect.xy + inUV * inInstanceUVRect.zw;
    vColor = inColor * inInstanceColor;

    vec4 world = vec4(worldXY, inInstanceModelRow0.w, 1.0);
    gl_Position = proj * world;
}
