# ID3 algorithm for learning classification
A Java implementation of the ID3 decision tree construction algorithm, built for the ECS629U Artificial Intelligence
module at Queen Mary University of London.

## Notes
Finding attributes with high entropy: 
1.  start with a compressing transform (e.g. time/frequency for sounds, wavelet or DCT for images, 
    movement vectors for video,…) to reduce redundancy 
2.  for numeric attributes, variance can often be used as a good surrogate for entropy, and is easier to compute 

Finding features with high mutual information with the classes: 
1.  expert knowledge may be available 
2.  Common sense works! e.g., don’t classify text documents based on frequency of “a”, “the”, “and”, ...