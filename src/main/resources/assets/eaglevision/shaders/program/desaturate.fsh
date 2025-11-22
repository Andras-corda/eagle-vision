#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);

    // Si le pixel est très lumineux et coloré, c'est une entité en Eagle Vision
    // On le préserve
    float maxChannel = max(max(color.r, color.g), color.b);
    float minChannel = min(min(color.r, color.g), color.b);
    float saturation = maxChannel - minChannel;

    // Si c'est très saturé et lumineux, on garde la couleur originale
    if (saturation > 0.3 && maxChannel > 0.5) {
        // Intensifier encore plus la couleur pour l'effet lumineux
        fragColor = vec4(color.rgb * 1.5, color.a);
    } else {
        // Sinon, désaturer
        float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
        vec3 desaturated = vec3(gray * 0.35, gray * 0.35, gray * 0.45);
        fragColor = vec4(desaturated, color.a);
    }
}