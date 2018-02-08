import Foundation
import Flutter

class TextureHandler: NSObject, FlutterTexture {
    
    var buffer: CMSampleBuffer?
    
    let textureRegistry: FlutterTextureRegistry
    let textureId: Int64?
    
    init(registry: TextureRegistry) {
        self.textureRegistry = textureRegistry
        self.textureId = textureRegistry.register(self)
    }
    
    func setImageBuffer(buffer: CMSampleBuffer) {
        // we're just going to use a dumb implementation for now that simply sets
        // the buffer and hopes for the best when it gets read.
        // Realistically, we should probably be using some sort of buffer so that
        // there isn't any chance of a problem happening.....
        self.buffer = buffer
        textureRegistry.textureFrameAvailable(<#T##textureId: Int64##Int64#>)
    }
    
    func copyPixelBuffer() -> Unmanaged<CVPixelBuffer> {
        // need to never block here, as we don't realy know which thread this is coming from
        // (quite likely the UI thread)
        
        // assume if this being called we're good to go...?
        let pixBuffer = CMSampleBufferGetImageBuffer(buffer!)!

        // for now we're just returning this as retained, but no idea if
        // that's right. Fun on a bun!
        return .passRetained(pixBuffer)
    }
    
    func clear() {
        buffer = nil
        if let textureId = textureId {
            textureRegistry.unregisterTexture(textureId)
        }
    }
    
    deinit {
        clear()
    }
    
}
