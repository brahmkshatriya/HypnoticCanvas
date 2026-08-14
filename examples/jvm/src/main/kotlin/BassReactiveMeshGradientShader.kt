import androidx.compose.ui.graphics.Color
import com.mikepenz.hypnoticcanvas.RuntimeEffect
import com.mikepenz.hypnoticcanvas.shaders.Shader

internal object BassReactiveMeshGradientShader : Shader {
    var bassProvider: () -> Float = { 0f }

    override val name: String = "Bass Reactive Mesh Gradient"
    override val authorName: String = "Mike Penz"
    override val authorUrl: String = "https://github.com/mikepenz/"
    override val credit: String = "Original mesh gradient inspired by React Mesh Gradient"
    override val license: String = "MIT License"
    override val licenseUrl: String = "https://opensource.org/license/mit"
    override val speedModifier: Float = 0.12f

    private val colors = arrayOf(
        Color(0xFF00D4FF),
        Color(0xFF7C4DFF),
        Color(0xFFFF4D8D),
        Color(0xFFFFC857),
        Color(0xFF0F172A),
    )

    private val colorUniforms = colors.flatMap {
        listOf(it.red, it.green, it.blue)
    }.toTypedArray().toFloatArray()

    override val sksl: String = """
uniform float uTime;
uniform vec3 uResolution;
uniform float uBass;

vec3 vColor;
const int MAX_COLORS = ${colors.size};
uniform vec3 uColor[MAX_COLORS];

vec4 permute(vec4 x) {
    return mod(((x * 34.0) + 1.0) * x, 289.0);
}

vec4 taylorInvSqrt(vec4 r) {
    return 1.79284291400159 - 0.85373472095314 * r;
}

float snoise(vec3 v) {
    const vec2 C = vec2(1.0 / 6.0, 1.0 / 3.0);
    const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);

    vec3 i = floor(v + dot(v, C.yyy));
    vec3 x0 = v - i + dot(i, C.xxx);

    vec3 g = step(x0.yzx, x0.xyz);
    vec3 l = 1.0 - g;
    vec3 i1 = min(g.xyz, l.zxy);
    vec3 i2 = max(g.xyz, l.zxy);

    vec3 x1 = x0 - i1 + 1.0 * C.xxx;
    vec3 x2 = x0 - i2 + 2.0 * C.xxx;
    vec3 x3 = x0 - 1.0 + 3.0 * C.xxx;

    i = mod(i, 289.0);
    vec4 p = permute(
        permute(permute(i.z + vec4(0.0, i1.z, i2.z, 1.0))
            + i.y + vec4(0.0, i1.y, i2.y, 1.0))
            + i.x + vec4(0.0, i1.x, i2.x, 1.0)
    );

    float n_ = 1.0 / 7.0;
    vec3 ns = n_ * D.wyz - D.xzx;

    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);
    vec4 x_ = floor(j * ns.z);
    vec4 y_ = floor(j - 7.0 * x_);

    vec4 x = x_ * ns.x + ns.yyyy;
    vec4 y = y_ * ns.x + ns.yyyy;
    vec4 h = 1.0 - abs(x) - abs(y);

    vec4 b0 = vec4(x.xy, y.xy);
    vec4 b1 = vec4(x.zw, y.zw);

    vec4 s0 = floor(b0) * 2.0 + 1.0;
    vec4 s1 = floor(b1) * 2.0 + 1.0;
    vec4 sh = -step(h, vec4(0.0));

    vec4 a0 = b0.xzyw + s0.xzyw * sh.xxyy;
    vec4 a1 = b1.xzyw + s1.xzyw * sh.zzww;

    vec3 p0 = vec3(a0.xy, h.x);
    vec3 p1 = vec3(a0.zw, h.y);
    vec3 p2 = vec3(a1.xy, h.z);
    vec3 p3 = vec3(a1.zw, h.w);

    vec4 norm = taylorInvSqrt(vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3)));
    p0 *= norm.x;
    p1 *= norm.y;
    p2 *= norm.z;
    p3 *= norm.w;

    vec4 m = max(0.6 - vec4(dot(x0, x0), dot(x1, x1), dot(x2, x2), dot(x3, x3)), 0.0);
    m = m * m;
    return 42.0 * dot(m * m, vec4(dot(p0, x0), dot(p1, x1), dot(p2, x2), dot(p3, x3)));
}

float hash1(float n) {
    return fract(sin(n * 127.1) * 43758.5453123);
}

vec2 hash2(float n) {
    return fract(sin(vec2(n, n + 1.0) * vec2(127.1, 311.7)) * vec2(43758.5453123, 22578.1459123));
}

vec4 main(vec2 fragCoord) {
    float mr = min(uResolution.x, uResolution.y);
    vec2 uv = (fragCoord * 2.4 - uResolution.xy) / mr;
    float bass = clamp(uBass, 0.0, 1.0);
    bass = smoothstep(0.05, 0.75, bass);
    float pulse = mix(0.16, 0.42, bass);
    float driftTime = uTime * pulse;
    float constantDrift = uTime * 0.05;
    float segment = floor(uTime / 4.0);
    float segmentT = smoothstep(0.0, 1.0, fract(uTime / 4.0));
    float angleA = mix(-1.0, 1.0, hash1(segment)) * 0.8;
    float angleB = mix(-1.0, 1.0, hash1(segment + 1.0)) * 0.8;
    float angle = mix(angleA, angleB, segmentT);
    mat2 orientation = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
    vec2 centerA = (hash2(segment) - 0.5) * 0.35;
    vec2 centerB = (hash2(segment + 1.0) - 0.5) * 0.35;
    vec2 center = mix(centerA, centerB, segmentT);

    vec2 base = orientation * ((uv - center) / 2.0);
    vec2 orbit = vec2(
        sin(constantDrift * 0.8),
        cos(constantDrift * 0.67)
    ) * 0.075;
    base += orbit;
    float tilt = -0.5 * uv.y;
    float incline = uv.x * 0.1;
        float offset = incline * mix(-0.25, 0.25, uv.y);
        float noise = snoise(vec3(
            base.x + driftTime * 0.03 + constantDrift * 0.14,
            base.y + constantDrift * 0.09,
            driftTime * 0.025 + constantDrift * 0.11
        ));
        noise = max(0.0, noise);
        vec3 pos = vec3(uResolution.x, uResolution.y, uResolution.z + noise * (0.08 + bass * 0.14) + tilt + incline + offset);

        vColor = uColor[MAX_COLORS - 1];
        for (int i = 0; i < MAX_COLORS - 1; i++) {
            float flow = 0.9 + float(i) * 0.08;
            float speed = (0.9 + float(i) * 0.05) * pulse;
            float seed = 1.0 + float(i) * 4.0;
            vec2 frequency = vec2(0.3, 0.7);
            float noiseFloor = 0.00001;
            float noiseCeil = 0.7 + float(i) * 0.02 + bass * 0.015;
            float gradient = smoothstep(
                noiseFloor,
                noiseCeil,
                snoise(vec3(
                    base.x * frequency.x + driftTime * 0.0007 * flow + constantDrift * 0.03,
                    base.y * frequency.y + constantDrift * 0.02,
                    driftTime * 0.0007 * speed + seed + constantDrift * 0.025
                ))
            );
            vColor = mix(vColor, uColor[i], gradient);
        }

    float glow = 0.012 + bass * 0.01 + 0.006 * (0.5 + 0.5 * sin(constantDrift * 0.9));
    vColor += vec3(glow * 0.35, glow * 0.2, glow * 0.45) * smoothstep(-0.2, 0.8, noise);
    return vec4(vColor, 1.0);
}
    """.trimIndent()

    override fun applyUniforms(runtimeEffect: RuntimeEffect, time: Float, width: Float, height: Float) {
        super.applyUniforms(runtimeEffect, time, width, height)
        runtimeEffect.setFloatUniform("uBass", bassProvider().coerceIn(0f, 1f))
        runtimeEffect.setFloatUniform("uColor", colorUniforms)
    }
}
