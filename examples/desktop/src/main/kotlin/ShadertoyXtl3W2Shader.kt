import com.mikepenz.hypnoticcanvas.RuntimeEffect
import com.mikepenz.hypnoticcanvas.shaders.Shader

internal object ShadertoyXtl3W2Shader : Shader {
    var musicLevelProvider: () -> Float = { 0f }

    override val name: String = "Daily: 1/3/15"
    override val authorName: String = "Hugh Kennedy"
    override val authorUrl: String = "https://www.shadertoy.com/user/hughsk"
    override val credit: String = "https://www.shadertoy.com/view/Xtl3W2"
    override val license: String = "Original Shadertoy license"
    override val licenseUrl: String = "https://www.shadertoy.com/terms"
    override val speedModifier: Float = 0.5f

    override val sksl: String = """
uniform vec3 uResolution;
uniform float uTime;
uniform float uBeat;

const float PI = 3.14159265;
const float F4 = 0.309016994374947451;

float audioTexture(vec2 uv) {
    float pulse = smoothstep(0.02, 0.85, uBeat);
    float fine = 0.5 + 0.5 * sin(uv.x * 40.0 + uTime * 8.0);
    float bass = 0.5 + 0.5 * sin(uv.x * 9.0 - uTime * 3.0);
    return clamp(0.12 + pulse * (0.62 + 0.18 * fine + 0.18 * bass), 0.0, 1.0);
}

float orenNayarDiffuse(vec3 lightDirection, vec3 viewDirection, vec3 surfaceNormal, float roughness, float albedo) {
    float LdotV = dot(lightDirection, viewDirection);
    float NdotL = dot(lightDirection, surfaceNormal);
    float NdotV = dot(surfaceNormal, viewDirection);
    float s = LdotV - NdotL * NdotV;
    float t = mix(1.0, max(NdotL, NdotV), step(0.0, s));
    float sigma2 = roughness * roughness;
    float A = 1.0 + sigma2 * (albedo / (sigma2 + 0.13) + 0.5 / (sigma2 + 0.33));
    float B = 0.45 * sigma2 / (sigma2 + 0.09);
    return albedo * max(0.0, NdotL) * (A + B * s / t) / PI;
}

float gaussianSpecular(vec3 lightDirection, vec3 viewDirection, vec3 surfaceNormal, float shininess) {
    vec3 H = normalize(lightDirection + viewDirection);
    float theta = acos(clamp(dot(H, surfaceNormal), -1.0, 1.0));
    float w = theta / shininess;
    return exp(-w * w);
}

float fogFactorExp2(float dist, float density) {
    float d = density * dist;
    return 1.0 - clamp(exp2(d * d * -1.442695), 0.0, 1.0);
}

vec4 mod289(vec4 x) {
    return x - floor(x * (1.0 / 289.0)) * 289.0;
}

float mod289(float x) {
    return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec4 permute(vec4 x) {
    return mod289(((x * 34.0) + 1.0) * x);
}

float permute(float x) {
    return mod289(((x * 34.0) + 1.0) * x);
}

vec4 taylorInvSqrt(vec4 r) {
    return 1.79284291400159 - 0.85373472095314 * r;
}

float taylorInvSqrt(float r) {
    return 1.79284291400159 - 0.85373472095314 * r;
}

vec4 grad4(float j, vec4 ip) {
    const vec4 ones = vec4(1.0, 1.0, 1.0, -1.0);
    vec4 p;
    p.xyz = floor(fract(vec3(j) * ip.xyz) * 7.0) * ip.z - 1.0;
    p.w = 1.5 - dot(abs(p.xyz), ones.xyz);
    vec4 s = vec4(lessThan(p, vec4(0.0)));
    p.xyz = p.xyz + (s.xyz * 2.0 - 1.0) * s.www;
    return p;
}

float snoise(vec4 v) {
    const vec4 C = vec4(0.138196601125011, 0.276393202250021, 0.414589803375032, -0.447213595499958);
    vec4 i = floor(v + dot(v, vec4(F4)));
    vec4 x0 = v - i + dot(i, C.xxxx);
    vec4 i0;
    vec3 isX = step(x0.yzw, x0.xxx);
    vec3 isYZ = step(x0.zww, x0.yyz);
    i0.x = isX.x + isX.y + isX.z;
    i0.yzw = 1.0 - isX;
    i0.y += isYZ.x + isYZ.y;
    i0.zw += 1.0 - isYZ.xy;
    i0.z += isYZ.z;
    i0.w += 1.0 - isYZ.z;

    vec4 i3 = clamp(i0, 0.0, 1.0);
    vec4 i2 = clamp(i0 - 1.0, 0.0, 1.0);
    vec4 i1 = clamp(i0 - 2.0, 0.0, 1.0);
    vec4 x1 = x0 - i1 + C.xxxx;
    vec4 x2 = x0 - i2 + C.yyyy;
    vec4 x3 = x0 - i3 + C.zzzz;
    vec4 x4 = x0 + C.wwww;

    i = mod289(i);
    float j0 = permute(permute(permute(permute(i.w) + i.z) + i.y) + i.x);
    vec4 j1 = permute(permute(permute(permute(
        i.w + vec4(i1.w, i2.w, i3.w, 1.0))
        + i.z + vec4(i1.z, i2.z, i3.z, 1.0))
        + i.y + vec4(i1.y, i2.y, i3.y, 1.0))
        + i.x + vec4(i1.x, i2.x, i3.x, 1.0));

    vec4 ip = vec4(1.0 / 294.0, 1.0 / 49.0, 1.0 / 7.0, 0.0);
    vec4 p0 = grad4(j0, ip);
    vec4 p1 = grad4(j1.x, ip);
    vec4 p2 = grad4(j1.y, ip);
    vec4 p3 = grad4(j1.z, ip);
    vec4 p4 = grad4(j1.w, ip);

    vec4 norm = taylorInvSqrt(vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3)));
    p0 *= norm.x;
    p1 *= norm.y;
    p2 *= norm.z;
    p3 *= norm.w;
    p4 *= taylorInvSqrt(dot(p4, p4));

    vec3 m0 = max(0.6 - vec3(dot(x0, x0), dot(x1, x1), dot(x2, x2)), 0.0);
    vec2 m1 = max(0.6 - vec2(dot(x3, x3), dot(x4, x4)), 0.0);
    m0 = m0 * m0;
    m1 = m1 * m1;
    return 49.0 * (
        dot(m0 * m0, vec3(dot(p0, x0), dot(p1, x1), dot(p2, x2))) +
        dot(m1 * m1, vec2(dot(p3, x3), dot(p4, x4)))
    );
}

void doCamera(out vec3 camPos, out vec3 camTar, float time, float mouseX) {
    float an = 10.0 * mouseX + 4.5 + time * 0.08;
    camPos = vec3(3.5 * sin(an), 1.0, 3.5 * cos(an));
    camTar = vec3(0.0, 0.0, 0.0);
}

vec3 doBackground() {
    return vec3(0.003, 0.003, 0.005);
}

float doModel(vec3 p) {
    float n = max(0.0, audioTexture(vec2(0.05, 0.0)) * 3.5 - 1.0);
    n = n * exp(snoise(vec4(p * 1.7, uTime * 2.3)));
    return length(p) - (1.0 + n * 0.04) * 0.9;
}

vec3 doMaterial(vec3 pos, vec3 nor) {
    return vec3(0.125, 0.1, 0.2) + (vec3(0.6, 0.9, 0.4) * 3.0 * clamp(length(pos) - 0.94, 0.0, 1.0));
}

float calcSoftshadow(vec3 ro, vec3 rd) {
    float res = 1.0;
    float t = 0.0001;
    for (int i = 0; i < 2; i++) {
        float h = doModel(ro + rd * t);
        res = min(res, 4.0 * h / t);
        t += clamp(h, 0.02, 2.0);
    }
    return clamp(res, 0.0, 1.0);
}

vec3 doLighting(vec3 pos, vec3 nor, vec3 rd, float dis, vec3 mal) {
    vec3 lin = vec3(0.0);
    vec3 view = normalize(-rd);
    vec3 lig1 = normalize(vec3(1.0, 0.7, 0.9));
    vec3 lig2 = normalize(vec3(1.0, 0.9, 0.9) * -1.0);

    float spc1 = gaussianSpecular(lig1, view, nor, 0.95) * 0.5;
    float dif1 = max(0.0, orenNayarDiffuse(lig1, view, nor, -20.1, 1.0));
    float sha1 = 0.75 + 0.25 * step(0.01, dif1);
    vec3 col1 = vec3(2.0, 4.2, 4.0);
    lin += col1 * spc1 + dif1 * col1 * sha1;

    float spc2 = gaussianSpecular(lig2, view, nor, 0.95);
    float dif2 = max(0.0, orenNayarDiffuse(lig2, view, nor, -20.1, 1.0));
    vec3 col2 = vec3(2.0, 0.05, 0.15);
    lin += col2 * spc2 + dif2 * col2 * sha1;
    lin += vec3(0.05);
    return mal * lin;
}

float calcIntersection(vec3 ro, vec3 rd) {
    const float maxd = 12.0;
    const float precis = 0.004;
    float h = precis * 2.0;
    float t = 0.0;
    float res = -1.0;
    for (int i = 0; i < 32; i++) {
        if (h < precis || t > maxd) break;
        h = doModel(ro + rd * t);
        t += h * 0.92;
    }
    if (t < maxd) res = t;
    return res;
}

vec3 calcNormal(vec3 pos) {
    return normalize(pos);
}

mat3 calcLookAtMatrix(vec3 ro, vec3 ta, float roll) {
    vec3 ww = normalize(ta - ro);
    vec3 uu = normalize(cross(ww, vec3(sin(roll), cos(roll), 0.0)));
    vec3 vv = normalize(cross(uu, ww));
    return mat3(uu, vv, ww);
}

vec4 main(vec2 fragCoord) {
    vec2 p = (-uResolution.xy + 2.0 * fragCoord.xy) / uResolution.y;
    float mouseX = 0.42 + uBeat * 0.08;
    vec3 ro;
    vec3 ta;
    doCamera(ro, ta, uTime, mouseX);
    mat3 camMat = calcLookAtMatrix(ro, ta, 0.0);
    vec3 rd = normalize(camMat * vec3(p.xy, 2.0));
    vec3 col = doBackground();

    float t = calcIntersection(ro, rd);
    if (t > -0.5) {
        vec3 pos = ro + t * rd;
        vec3 nor = calcNormal(pos);
        vec3 mal = doMaterial(pos, nor);
        vec3 lcl = doLighting(pos, nor, rd, t, mal);
        col = mix(lcl, col, fogFactorExp2(t, 0.1));
    }

    col = pow(clamp(col, 0.0, 1.0), vec3(0.4545));
    col += dot(p, p * 0.035);
    col.r = smoothstep(0.1, 1.1, col.r);
    col.g = pow(col.g, 1.1);
    return vec4(col, 1.0);
}
    """.trimIndent()

    override fun applyUniforms(runtimeEffect: RuntimeEffect, time: Float, width: Float, height: Float) {
        super.applyUniforms(runtimeEffect, time, width, height)
        runtimeEffect.setFloatUniform("uBeat", musicLevelProvider().coerceIn(0f, 1f))
    }
}
