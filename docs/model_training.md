# Model Training Process

## Dataset Preparation
- **Source**: Recyclable Solid Waste Dataset from Kaggle
- **Classes**: 3 categories (glass, metal,plastic)
- **Total Images**: 2,500+ across multiple backgrounds
- **Split**: 80% training, 20% testing
- **Augmentation**: Rotation, zoom, flip, brightness variation

## Edge Impulse Pipeline
Input (96x96 RGB) → Image Processing → Transfer Learning (MobileNetV2) → Classification Output


## Training Configuration
- **Base Model**: MobileNetV1 (pre-trained on ImageNet)
- **Input Size**: 96x96 pixels
- **Epochs**: 35
- **Learning Rate**: 0.0005
- **Batch Size**: 16
- **Optimizer**: Adam
- **Loss Function**: Categorical Crossentropy

## Training Results
- **Final Training Accuracy**: 91.2%
- **Validation Accuracy**: 90.15%
- **Training Time**: ~45 minutes


## Key Decisions
1. **MobileNetV1**: Chosen for mobile optimization and good accuracy balance
2. **96x96 Input**: Optimal for mobile performance while maintaining accuracy
3. **Int8 Quantization**: Essential for mobile deployment and speed