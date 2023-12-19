package GraphXings.NewFiles;

import GraphXings.Data.Vertex;

class VertexSamplePair {
    Vertex vertex;
    int sample;

    VertexSamplePair(Vertex vertex, int sample) {
        this.vertex = vertex;
        this.sample = sample;
    }
    
    public int getSample() {
        return sample;
    }
    public Vertex getVertex() {
        return vertex;
    }
}
