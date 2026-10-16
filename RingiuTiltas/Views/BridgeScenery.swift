import SwiftUI

/// Full-screen animated scene of the Jūra bridge at Ringiai. The water surface
/// position is driven by the live level (cm): below 190 the water stays under
/// the structure, from 190 it reaches the deck underside, above 215 it flows
/// over the road. Every state has its own animated elements:
/// - dry: rotating sun rays, gliding birds, calm shimmering water
/// - wet: waves lapping at the deck, spray droplets, light drizzle, clouds
/// - flooded: water over the roadway, floating debris, heavy rain, big waves
struct BridgeScenery: View {
    let level: Double?
    let state: BridgeState

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 30.0)) { timeline in
            Canvas { context, size in
                var ctx = context
                draw(in: &ctx, size: size,
                     time: timeline.date.timeIntervalSinceReferenceDate)
            }
        }
        .ignoresSafeArea()
    }

    // MARK: - Scene layout

    /// Maps the reading to a vertical waterline position.
    private func waterY(level: Double?, height: CGFloat) -> CGFloat {
        let h = height
        func lerp(_ from: (Double, CGFloat), _ to: (Double, CGFloat), at v: Double) -> CGFloat {
            let f = CGFloat((v - from.0) / (to.0 - from.0)).clamped(to: 0...1)
            return from.1 + f * (to.1 - from.1)
        }
        guard let level else { return h * 0.75 }
        switch level {
        case ..<150: return h * 0.97
        case 150..<190: return lerp((150, h * 0.97), (190, h * 0.66), at: level)
        case 190..<215: return lerp((190, h * 0.66), (215, h * 0.57), at: level)
        case 215..<240: return lerp((215, h * 0.57), (240, h * 0.26), at: level)
        default: return h * 0.26
        }
    }

    private func draw(in context: inout GraphicsContext, size: CGSize, time: Double) {
        let skyColors: [Color]
        switch state {
        case .dry:
            skyColors = [Color(red: 0.36, green: 0.66, blue: 0.94),
                         Color(red: 0.72, green: 0.87, blue: 0.99)]
        case .wet:
            skyColors = [Color(red: 0.50, green: 0.62, blue: 0.74),
                         Color(red: 0.72, green: 0.80, blue: 0.87)]
        case .flooded:
            skyColors = [Color(red: 0.14, green: 0.20, blue: 0.32),
                         Color(red: 0.30, green: 0.40, blue: 0.55)]
        case .unknown:
            skyColors = [Color(red: 0.62, green: 0.63, blue: 0.66),
                         Color(red: 0.80, green: 0.81, blue: 0.83)]
        }
        context.fill(Path(CGRect(origin: .zero, size: size)),
                     with: .linearGradient(Gradient(colors: skyColors),
                                           startPoint: .zero,
                                           endPoint: CGPoint(x: 0, y: size.height)))

        let waterLine = waterY(level: level, height: size.height)

        if state == .dry {
            drawSun(&context, size: size, time: time)
            drawBirds(&context, size: size, time: time)
        } else {
            drawClouds(&context, size: size, time: time, dark: state == .flooded)
        }

        drawBridge(&context, size: size)

        if state == .flooded {
            drawDebris(&context, size: size, waterY: waterLine, time: time)
        }

        drawWater(&context, size: size, waterY: waterLine, time: time)

        if state == .wet {
            drawSpray(&context, size: size, waterY: waterLine, time: time)
        }
        if state == .flooded {
            drawRain(&context, size: size, time: time, heavy: true)
        } else if state == .wet {
            drawRain(&context, size: size, time: time, heavy: false)
        }
    }

    // MARK: - Sky

    private func drawSun(_ context: inout GraphicsContext, size: CGSize, time: Double) {
        let center = CGPoint(x: size.width * 0.82, y: size.height * 0.13)
        let radius = size.width * 0.06

        let glow = Path(ellipseIn: CGRect(x: center.x - radius * 2.2,
                                          y: center.y - radius * 2.2,
                                          width: radius * 4.4, height: radius * 4.4))
        context.fill(glow, with: .color(.yellow.opacity(0.25)))

        for i in 0..<8 {
            let angle = Double(i) / 8 * 2 * .pi + time * 0.15
            let dx = CGFloat(cos(angle)) * radius * 1.5
            let dy = CGFloat(sin(angle)) * radius * 1.5
            var ray = Path()
            ray.move(to: CGPoint(x: center.x + dx * 0.8, y: center.y + dy * 0.8))
            ray.addLine(to: CGPoint(x: center.x + dx * 1.25, y: center.y + dy * 1.25))
            context.stroke(ray, with: .color(.yellow.opacity(0.8)),
                           style: StrokeStyle(lineWidth: 3, lineCap: .round))
        }

        let disc = Path(ellipseIn: CGRect(x: center.x - radius, y: center.y - radius,
                                          width: radius * 2, height: radius * 2))
        context.fill(disc, with: .color(.yellow))
    }

    private func drawBirds(_ context: inout GraphicsContext, size: CGSize, time: Double) {
        for i in 0..<4 {
            let speed = 18.0 + Double(i) * 6
            let x = CGFloat((time * speed).truncatingRemainder(dividingBy: Double(size.width + 80))) - 40
            let y = size.height * (0.20 + 0.05 * CGFloat(i % 3)) + CGFloat(sin(time * 1.5 + Double(i))) * 6
            let flap = CGFloat(sin(time * 6 + Double(i) * 1.7)) * 5

            var bird = Path()
            bird.move(to: CGPoint(x: x - 7, y: y - flap))
            bird.addQuadCurve(to: CGPoint(x: x, y: y + 2),
                              control: CGPoint(x: x - 3, y: y - 4))
            bird.addQuadCurve(to: CGPoint(x: x + 7, y: y - flap),
                              control: CGPoint(x: x + 3, y: y - 4))
            context.stroke(bird, with: .color(.black.opacity(0.6)),
                           style: StrokeStyle(lineWidth: 2, lineCap: .round))
        }
    }

    private func drawClouds(_ context: inout GraphicsContext, size: CGSize,
                            time: Double, dark: Bool) {
        let color = dark ? Color(white: 0.42) : Color(white: 0.88)
        let alpha = dark ? 0.92 : 0.8
        let clouds: [(CGFloat, CGFloat, CGFloat)] = [(0.20, 0.11, 1.0),
                                                     (0.55, 0.07, 0.8),
                                                     (0.80, 0.16, 1.15)]
        for (fx, fy, scale) in clouds {
            let drift = CGFloat(sin(time * 0.05 + Double(fx) * 6) * 10)
            let cx = size.width * fx + drift
            let cy = size.height * fy

            var cloud = Path()
            cloud.addEllipse(in: CGRect(x: cx - 70 * scale, y: cy - 14 * scale,
                                        width: 140 * scale, height: 44 * scale))
            cloud.addEllipse(in: CGRect(x: cx - 45 * scale, y: cy - 34 * scale,
                                        width: 90 * scale, height: 52 * scale))
            cloud.addEllipse(in: CGRect(x: cx + 15 * scale, y: cy - 26 * scale,
                                        width: 80 * scale, height: 44 * scale))
            context.fill(cloud, with: .color(color.opacity(alpha)))
        }
    }

    // MARK: - Bridge

    private func drawBridge(_ context: inout GraphicsContext, size: CGSize) {
        let w = size.width
        let h = size.height
        let deckLeft = w * 0.10
        let deckRight = w * 0.90
        let deckTop = h * 0.55
        let deckBottom = h * 0.585

        var piers = Path()
        for cx in [w * 0.24, w * 0.50, w * 0.76] {
            let pierWidth = w * 0.05
            piers.addRoundedRect(in: CGRect(x: cx - pierWidth / 2, y: deckBottom,
                                            width: pierWidth, height: h - deckBottom),
                                 cornerSize: CGSize(width: pierWidth * 0.12,
                                                    height: pierWidth * 0.12))
        }
        context.fill(piers, with: .color(Color(red: 0.35, green: 0.36, blue: 0.39)))

        var deck = Path()
        deck.addRoundedRect(in: CGRect(x: deckLeft, y: deckTop,
                                       width: deckRight - deckLeft,
                                       height: deckBottom - deckTop),
                            cornerSize: CGSize(width: 6, height: 6))
        context.fill(deck, with: .color(Color(red: 0.28, green: 0.30, blue: 0.33)))

        let railTop = deckTop - h * 0.055
        var posts = Path()
        let count = 14
        for i in 0...count {
            let x = deckLeft + (deckRight - deckLeft) * CGFloat(i) / CGFloat(count)
            posts.move(to: CGPoint(x: x, y: railTop))
            posts.addLine(to: CGPoint(x: x, y: deckTop))
        }
        context.stroke(posts, with: .color(.white.opacity(0.85)),
                       style: StrokeStyle(lineWidth: max(2, w * 0.004)))

        var rail = Path()
        rail.move(to: CGPoint(x: deckLeft, y: railTop))
        rail.addLine(to: CGPoint(x: deckRight, y: railTop))
        context.stroke(rail, with: .color(.white.opacity(0.9)),
                       style: StrokeStyle(lineWidth: max(3, w * 0.008)))
    }

    // MARK: - Water

    private func waveY(x: CGFloat, base: CGFloat, amp: CGFloat,
                       period: CGFloat, phase: Double) -> CGFloat {
        base + CGFloat(sin(Double(x / period) * 2 * .pi + phase)) * amp
    }

    private func drawWater(_ context: inout GraphicsContext, size: CGSize,
                           waterY: CGFloat, time: Double) {
        var bodyWater = Path()
        bodyWater.move(to: CGPoint(x: 0, y: waterY))
        bodyWater.addLine(to: CGPoint(x: size.width, y: waterY))
        bodyWater.addLine(to: CGPoint(x: size.width, y: size.height))
        bodyWater.addLine(to: CGPoint(x: 0, y: size.height))
        bodyWater.closeSubpath()

        let shades: [Color]
        switch state {
        case .flooded:
            shades = [Color(red: 0.10, green: 0.28, blue: 0.45).opacity(0.88),
                      Color(red: 0.06, green: 0.16, blue: 0.30)]
        case .wet:
            shades = [Color(red: 0.15, green: 0.40, blue: 0.62).opacity(0.85),
                      Color(red: 0.08, green: 0.25, blue: 0.45)]
        case .unknown:
            shades = [Color(red: 0.45, green: 0.58, blue: 0.66).opacity(0.75),
                      Color(red: 0.30, green: 0.42, blue: 0.52)]
        case .dry:
            shades = [Color(red: 0.12, green: 0.42, blue: 0.68).opacity(0.9),
                      Color(red: 0.10, green: 0.30, blue: 0.52)]
        }
        context.fill(bodyWater, with: .linearGradient(Gradient(colors: shades),
                                                      startPoint: CGPoint(x: 0, y: waterY),
                                                      endPoint: CGPoint(x: 0, y: size.height)))

        let speeds: [Double] = state == .flooded ? [1.6, 2.4] : [0.5, 0.8]
        let amps: [CGFloat] = state == .flooded ? [10, 7] : (state == .wet ? [8, 5] : [4, 3])
        let periods: [CGFloat] = state == .flooded ? [130, 90] : [150, 100]

        for i in 0..<2 {
            let base = waterY + CGFloat(i) * 7
            let phase = time * speeds[i] + Double(i) * 2.1
            var crest = Path()
            crest.move(to: CGPoint(x: 0, y: waveY(x: 0, base: base, amp: amps[i],
                                                  period: periods[i], phase: phase)))
            var x: CGFloat = 0
            while x < size.width + 8 {
                x += 8
                crest.addLine(to: CGPoint(x: x, y: waveY(x: x, base: base, amp: amps[i],
                                                         period: periods[i], phase: phase)))
            }
            crest.addLine(to: CGPoint(x: size.width, y: size.height))
            crest.addLine(to: CGPoint(x: 0, y: size.height))
            crest.closeSubpath()

            let crestColor = Color(red: 0.40 + 0.15 * Double(i),
                                   green: 0.62,
                                   blue: 0.85)
                .opacity(state == .flooded ? 0.5 : 0.35)
            context.fill(crest, with: .color(crestColor))
        }
    }

    // MARK: - Effects

    private func drawSpray(_ context: inout GraphicsContext, size: CGSize,
                           waterY: CGFloat, time: Double) {
        for i in 0..<24 {
            let seedA = Double((i * 911) % 991) / 991.0
            let seedB = Double((i * 677) % 983) / 983.0
            let period = 0.7 + seedB * 0.7
            let progress = (time / period + seedA).truncatingRemainder(dividingBy: 1.0)
            let x = CGFloat(seedA) * size.width
            let bounce = CGFloat(sin(progress * .pi))
            let y = waterY - bounce * (6 + CGFloat(seedB) * 12)
            let drop = Path(ellipseIn: CGRect(x: x, y: y, width: 3, height: 3))
            context.fill(drop, with: .color(.white.opacity(0.75 * (1 - progress))))
        }
    }

    private func drawDebris(_ context: inout GraphicsContext, size: CGSize,
                            waterY: CGFloat, time: Double) {
        for i in 0..<3 {
            let speed = 60.0 + Double(i) * 25
            let cycle = (time * speed).truncatingRemainder(dividingBy: Double(size.width + 120))
            let x = CGFloat(cycle) - 60
            let bob = CGFloat(sin(time * 2.2 + Double(i))) * 5
            let y = waterY + bob + CGFloat(i) * 6 - 6

            var log = Path()
            log.addRoundedRect(in: CGRect(x: x, y: y, width: 60, height: 9),
                               cornerSize: CGSize(width: 4.5, height: 4.5))
            let wood = Color(red: 0.45 + 0.05 * Double(i), green: 0.30, blue: 0.20)
            context.fill(log, with: .color(wood))
        }
    }

    private func drawRain(_ context: inout GraphicsContext, size: CGSize,
                          time: Double, heavy: Bool) {
        let count = heavy ? 90 : 40
        for i in 0..<count {
            let seedX = Double((i * 137) % 997) / 997.0
            let seedV = Double((i * 40503) % 983) / 983.0
            let progress = (time * (0.9 + seedV * 0.6) + seedX)
                .truncatingRemainder(dividingBy: 1.0)
            let x = CGFloat(seedX) * size.width + CGFloat(sin(Double(i) + time) * 6)
            let y = CGFloat(progress) * size.height

            var drop = Path()
            drop.move(to: CGPoint(x: x, y: y))
            drop.addLine(to: CGPoint(x: x + 3, y: y + 16))
            context.stroke(drop, with: .color(.white.opacity(heavy ? 0.35 : 0.25)),
                           style: StrokeStyle(lineWidth: 2, lineCap: .round))
        }
    }
}

private extension CGFloat {
    func clamped(to range: ClosedRange<CGFloat>) -> CGFloat {
        min(max(self, range.lowerBound), range.upperBound)
    }
}